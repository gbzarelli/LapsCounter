package br.com.helpdev.lapscounter.wear.sync

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.helpdev.lapscounter.wear.data.*
import br.com.helpdev.lapscounter.wear.timer.ElapsedRealtimeClock
import br.com.helpdev.lapscounter.wear.timer.WatchTimerController
import com.google.android.gms.common.data.DataHolder
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataItemAsset
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataItemBuffer
import com.google.android.gms.wearable.Node
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActivitySyncRecoveryTest {
    private val peer = object : Node {
        override fun getId() = "phone"
        override fun getDisplayName() = "Phone"
        override fun isNearby() = true
    }

    @Test fun connectionCallbacksRecoverFailedUploadAndPersistPreviouslyDeliveredAck() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        val database = Room.inMemoryDatabaseBuilder(context, WatchDatabase::class.java).build()
        try {
            val repository = WatchActivityRepository(database)
            val timer = WatchTimerController(ElapsedRealtimeClock { 1000 }, "swimming", 25f)
            timer.start()
            val snapshot = timer.stop()
            repository.saveCompleted(snapshot, "Swim")
            var failPut = true
            var failAckWrite = true
            val store = object : WatchActivityStore by repository {
                override suspend fun markSynced(id: String, flavorId: String) {
                    if (failAckWrite) {
                        failAckWrite = false
                        error("transient Room failure")
                    }
                    repository.markSynced(id, flavorId)
                }
            }
            val sent = mutableListOf<String>()
            var storedAcks = emptyList<ActivityAcknowledgement>()
            val failures = mutableListOf<Exception>()
            val coordinator = ActivitySyncCoordinator(
                ActivitySyncOutbox(store, ActivitySyncTransport {
                    sent += it.activityId
                    if (failPut) error("initial put failed")
                }, "swimming"),
                ActivityAckSource { storedAcks },
                failures::add,
            )
            val service = object : ActivityAckListenerService() {
                override fun runSync(action: suspend (ActivitySyncCoordinator) -> Unit) {
                    runBlocking { action(coordinator) }
                }
            }
            coordinator.recover()
            assertEquals(listOf(snapshot.id), sent)
            failPut = false
            // The real background callback, with no Save or Activity resume in between.
            service.onPeerConnected(peer)
            assertEquals(listOf(snapshot.id, snapshot.id), sent)
            assertEquals(WatchSyncStatus.PENDING, repository.activity(snapshot.id)!!.activity.syncStatus)

            val ack = ActivityAcknowledgement("/activity-acks/${snapshot.id}", snapshot.id, 1)
            storedAcks = listOf(ack)
            coordinator.receive(ack)
            assertEquals("transient Room failure", failures.single().message)
            assertEquals(WatchSyncStatus.PENDING, repository.activity(snapshot.id)!!.activity.syncStatus)
            service.onConnectedNodes(mutableListOf(peer))
            assertEquals(WatchSyncStatus.SYNCED, repository.activity(snapshot.id)!!.activity.syncStatus)
            assertEquals(2, sent.size)
            assertEquals(1, repository.history().size)
        } finally {
            database.close()
        }
    }

    @Test fun ackQueryUsesPrefixAndReleasesReturnedBuffer() = runBlocking {
        val buffer = DataItemBuffer(DataHolder.empty(0))
        var queried = false
        val source = DataLayerActivityAckSource { uri, filter ->
            assertEquals(Uri.parse("wear://*/activity-acks/"), uri)
            assertEquals(DataClient.FILTER_PREFIX, filter)
            queried = true
            Tasks.forResult(buffer)
        }
        assertTrue(source.acknowledgements().isEmpty())
        assertTrue(queried)
        assertTrue(buffer.isClosed)
    }

    @Test fun ackQueryReleasesLateBufferAfterCallerCancellation() = runBlocking {
        val result = TaskCompletionSource<DataItemBuffer>()
        val source = DataLayerActivityAckSource { _, _ -> result.task }
        val job = launch { source.acknowledgements() }
        yield()
        job.cancelAndJoin()
        val buffer = DataItemBuffer(DataHolder.empty(0))
        result.setResult(buffer)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertTrue(buffer.isClosed)
    }

    @Test fun decoderCopiesAckMetadataAndRejectsMalformedData() {
        val id = "123e4567-e89b-12d3-a456-426614174000"
        val request = PutDataMapRequest.create("/activity-acks/$id")
        request.dataMap.putString("activityId", id)
        request.dataMap.putInt("schemaVersion", 1)
        fun item(bytes: ByteArray?) = object : DataItem {
            override fun getUri() = Uri.parse("wear://phone/activity-acks/$id")
            override fun getData() = bytes
            override fun setData(data: ByteArray?): DataItem = error("Read-only test item")
            override fun getAssets() = emptyMap<String, DataItemAsset>()
            override fun freeze(): DataItem = this
            override fun isDataValid() = true
        }
        assertEquals(ActivityAcknowledgement("/activity-acks/$id", id, 1),
            decodeActivityAcknowledgement(item(request.asPutDataRequest().data)))
        assertNull(decodeActivityAcknowledgement(item(byteArrayOf(0))))
    }

}
