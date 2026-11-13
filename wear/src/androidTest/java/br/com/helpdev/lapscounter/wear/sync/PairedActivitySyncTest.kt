package br.com.helpdev.lapscounter.wear.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.helpdev.lapscounter.sport.SportProfileProvider
import br.com.helpdev.lapscounter.wear.data.WatchActivityRepository
import br.com.helpdev.lapscounter.wear.data.WatchDatabase
import br.com.helpdev.lapscounter.wear.data.WatchSyncStatus
import br.com.helpdev.lapscounter.wear.timer.ElapsedRealtimeClock
import br.com.helpdev.lapscounter.wear.timer.WatchTimerController
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Opt-in live test. Use a fresh pairedActivityId and run save offline, then verify after pairing. */
@RunWith(AndroidJUnit4::class)
class PairedActivitySyncTest {
    @Test fun offlineSessionReachesPhoneAndAckRetainsWatchHistory() = runBlocking {
        val arguments = InstrumentationRegistry.getArguments()
        val phase = arguments.getString("pairedPhase")
        assumeTrue("Requires pairedPhase=save|verify and pairedActivityId; see release guide", phase != null)
        require(phase == "save" || phase == "verify")
        val id = requireNotNull(arguments.getString("pairedActivityId"))
        require(UUID.fromString(id).toString() == id)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sport = SportProfileProvider.get().sportId.name.lowercase(Locale.ROOT)
        val database = WatchDatabase.open(context)
        try {
            val repository = WatchActivityRepository(database)
            val client = Wearable.getDataClient(context)
            val outbox = ActivitySyncOutbox(repository,
                DataLayerActivitySyncTransport(PlayServicesDataClientAdapter(client)), sport)
            if (phase == "save") {
                val capability = Tasks.await(Wearable.getCapabilityClient(context).getCapability(
                    "lapscounter_phone", CapabilityClient.FILTER_REACHABLE), 15, TimeUnit.SECONDS)
                assertTrue("Disconnect/uninstall the matching phone before saving", capability.nodes.isEmpty())
                assertNull("Use a fresh UUID for each paired run", repository.activity(id))
                var now = 1_000L
                val timer = WatchTimerController(ElapsedRealtimeClock { now }, sport,
                    SportProfileProvider.get().defaultLapDistanceMeters,
                    wallClockMillis = { 1_700_000_000_000 })
                timer.start()
                now = 5_000
                timer.recordLap()
                now = 8_000
                repository.saveCompleted(timer.stop().copy(id = id), "Paired $sport")
                outbox.retryPending()
                assertEquals(WatchSyncStatus.PENDING, repository.activity(id)!!.activity.syncStatus)
            } else {
                assertEquals(listOf(4_000L, 3_000L), repository.activity(id)!!.laps.map { it.durationMillis })
                val coordinator = ActivitySyncCoordinator(outbox, DataLayerActivityAckSource(client))
                withTimeout(60_000) {
                    while (repository.activity(id)!!.activity.syncStatus != WatchSyncStatus.SYNCED) {
                        coordinator.recover()
                        delay(500)
                    }
                }
                // Reopen the same production database after the real phone ACK.
                WatchDatabase.open(context).let { reopened ->
                    try {
                        val copy = WatchActivityRepository(reopened).activity(id)!!
                        assertEquals(id, copy.activity.id)
                        assertEquals(WatchSyncStatus.SYNCED, copy.activity.syncStatus)
                        assertEquals(listOf(4_000L, 3_000L), copy.laps.map { it.durationMillis })
                    } finally { reopened.close() }
                }
            }
        } finally { database.close() }
    }
}
