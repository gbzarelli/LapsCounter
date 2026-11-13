package br.com.helpdev.lapscounter.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.helpdev.lapscounter.model.dao.ActivityDao
import br.com.helpdev.lapscounter.model.entity.ActivityEntity
import br.com.helpdev.lapscounter.model.entity.ChronometerEntity
import br.com.helpdev.lapscounter.model.entity.LapEntity
import br.com.helpdev.lapscounter.model.repository.ActivityRepository
import br.com.helpdev.lapscounter.sport.SportProfileProvider
import io.realm.Realm
import io.realm.RealmConfiguration
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ActivitySyncRealmIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun record() = ActivitySyncRecord(1, SportProfileProvider.get().sportId.name.lowercase(java.util.Locale.ROOT),
        "123e4567-e89b-12d3-a456-426614174000", "Watch activity", null,
        1_700_000_000_000, 50f, 50f, false, 7_000, 500,
        listOf(ActivitySyncLap(1, 4_000, 4_000, false), ActivitySyncLap(2, 3_000, 7_000, true)))

    @Test fun repeatedDeliveryCommitsOneActivityAndOrderedLapsBeforeAck() {
        withIsolatedRealm { config ->
            val acknowledgements = mutableListOf<String>()
            repeat(2) { delivery ->
                val done = CountDownLatch(1)
                var error: Throwable? = null
                instrumentation.runOnMainSync {
                    val receiver = ActivitySyncIngestion(
                        { r, success, failure -> ActivityRepository(ActivityDao()).importSynced(r, success, failure) },
                        { id, version ->
                            Realm.getInstance(config).use { realm ->
                                assertEquals(1L, realm.where(ActivityEntity::class.java).count())
                                assertEquals(record().activityId, realm.where(ActivityEntity::class.java).findFirst()!!.id)
                                assertEquals(listOf(4_000L, 3_000L), realm.where(ActivityEntity::class.java).findFirst()!!.chronometer!!.laps.map { it.runningTime })
                            }
                            assertEquals(1, version)
                            acknowledgements.add(id)
                            done.countDown()
                        },
                        { error = it; done.countDown() }
                    )
                    receiver.receive("/activities/${record().activityId}", record().activityId, 1, ActivitySyncCodec.encode(record()))
                    assertEquals(delivery, acknowledgements.size)
                }
                assertTrue(done.await(10, TimeUnit.SECONDS))
                assertNull(error)
            }
            assertEquals(listOf(record().activityId, record().activityId), acknowledgements)
            Realm.getInstance(config).use { realm ->
                assertEquals(1L, realm.where(ActivityEntity::class.java).count())
                assertEquals(1L, realm.where(ChronometerEntity::class.java).count())
                assertEquals(2L, realm.where(LapEntity::class.java).count())
            }
        }
    }

    @Test fun invalidPayloadMetadataAndFlavorNeverWriteOrAcknowledge() {
        withIsolatedRealm { config ->
            var acknowledgements = 0
            var failures = 0
            instrumentation.runOnMainSync {
                val receiver = ActivitySyncIngestion(
                    { r, success, failure -> ActivityRepository(ActivityDao()).importSynced(r, success, failure) },
                    { _, _ -> acknowledgements++ }, { failures++ })
                val r = record()
                val bytes = ActivitySyncCodec.encode(r)
                receiver.receive("/activities/${r.activityId}", r.activityId, 2, bytes)
                receiver.receive("/activities/other", r.activityId, 1, bytes)
                receiver.receive("/activities/123e4567-e89b-12d3-a456-426614174001", "123e4567-e89b-12d3-a456-426614174001", 1, bytes)
                receiver.receive("/activities/${r.activityId}", r.activityId, 1, byteArrayOf(1))
                receiver.receive("/activities/${r.activityId}", r.activityId, 1, ActivitySyncCodec.encode(r.copy(sportId = if (r.sportId == "running") "cycling" else "running")))
                receiver.receive("/activities/${r.activityId}", r.activityId, 1, ActivitySyncCodec.encode(r.copy(runningTimeMillis = 6_999)))
            }
            assertEquals(6, failures)
            assertEquals(0, acknowledgements)
            Realm.getInstance(config).use { assertEquals(0L, it.where(ActivityEntity::class.java).count()) }
        }
    }

    // Run this same matrix under each connected<Flavor>DebugAndroidTest task.
    // A missing flavor rejection or UUID deduplication must fail against real Realm rows.
    @Test fun allSportPayloadsReachOnlyMatchingFlavorHistoryOnce() {
        withIsolatedRealm { config ->
            val matching = record().sportId
            for ((index, sport) in listOf("swimming", "running", "cycling").withIndex()) {
                val payload = record().copy(sportId = sport,
                    activityId = "123e4567-e89b-12d3-a456-42661417400$index")
                repeat(2) {
                    val done = CountDownLatch(1)
                    var acknowledged = false
                    var rejected = false
                    instrumentation.runOnMainSync {
                        ActivitySyncIngestion(
                            { r, success, failure -> ActivityRepository(ActivityDao()).importSynced(r, success, failure) },
                            { id, version ->
                                assertEquals(payload.activityId, id)
                                assertEquals(1, version)
                                acknowledged = true
                                done.countDown()
                            },
                            { rejected = true; done.countDown() }
                        ).receive("/activities/${payload.activityId}", payload.activityId, 1, ActivitySyncCodec.encode(payload))
                    }
                    assertTrue(done.await(10, TimeUnit.SECONDS))
                    assertEquals(sport == matching, acknowledged)
                    assertEquals(sport != matching, rejected)
                }
            }
            Realm.getInstance(config).use { realm ->
                val activity = realm.where(ActivityEntity::class.java).findAll().single()
                val expectedId = "123e4567-e89b-12d3-a456-42661417400${listOf("swimming", "running", "cycling").indexOf(matching)}"
                assertEquals(expectedId, activity.id)
                assertEquals(listOf(4_000L, 3_000L), activity.chronometer!!.laps.map { it.runningTime })
                assertEquals(1L, realm.where(ChronometerEntity::class.java).count())
                assertEquals(2L, realm.where(LapEntity::class.java).count())
            }
        }
    }

    @Test fun realmTransactionSetupFailureNeverAcknowledges() {
        withIsolatedRealm { config ->
            var acknowledgements = 0
            var error: Throwable? = null
            // Instrumentation's test thread has no Looper: Realm must refuse callback setup.
            assertNull(android.os.Looper.myLooper())
            val receiver = ActivitySyncIngestion(
                { r, success, failure -> ActivityRepository(ActivityDao()).importSynced(r, success, failure) },
                { _, _ -> acknowledgements++ }, { error = it })
            val r = record()
            receiver.receive("/activities/${r.activityId}", r.activityId, 1, ActivitySyncCodec.encode(r))
            assertNotNull(error)
            assertEquals(0, acknowledgements)
            Realm.getInstance(config).use { assertEquals(0L, it.where(ActivityEntity::class.java).count()) }
        }
    }

    @Test fun transactionBodyFailureRestoresReplacedGraphAndNeverAcknowledges() {
        withIsolatedRealm { config ->
            val done = CountDownLatch(1)
            var error: Throwable? = null
            var acknowledgements = 0
            var source: Realm? = null
            Realm.getInstance(config).use { seed ->
                seed.executeTransaction { it.insertOrUpdate(ActivitySyncImporter.toActivityEntity(record(), record().sportId)) }
            }
            try {
                instrumentation.runOnMainSync {
                    val seed = Realm.getInstance(config).also { source = it }
                    // A managed child belongs to this thread. Copying it on Realm's async
                    // transaction thread fails inside insertOrUpdate, AFTER old graph deletion.
                    val invalidReplacement = ActivitySyncImporter.toActivityEntity(record().copy(name = "Replacement"), record().sportId)
                    invalidReplacement.chronometer = seed.where(ActivityEntity::class.java).findFirst()!!.chronometer
                    ActivitySyncIngestion(
                        { _, success, failure -> ActivityDao().importSynced(invalidReplacement, success, failure) },
                        { _, _ -> acknowledgements++; done.countDown() },
                        { error = it; done.countDown() },
                    ).receive("/activities/${record().activityId}", record().activityId, 1, ActivitySyncCodec.encode(record()))
                }
                assertTrue(done.await(10, TimeUnit.SECONDS))
                val transactionFailure = requireNotNull(error)
                assertTrue(transactionFailure.stackTraceToString(), transactionFailure.stackTraceToString().contains("insertOrUpdate"))
                assertEquals(0, acknowledgements)
                Realm.getInstance(config).use { realm ->
                    val activity = realm.where(ActivityEntity::class.java).findAll().single()
                    assertEquals(record().activityId, activity.id)
                    assertEquals("Watch activity", activity.name)
                    assertEquals(7000L, activity.chronometer!!.runningTime)
                    assertEquals(500L, activity.chronometer!!.pausedTime)
                    assertEquals(listOf(4000L, 3000L), activity.chronometer!!.laps.map { it.runningTime })
                    assertEquals(1L, realm.where(ChronometerEntity::class.java).count())
                    assertEquals(2L, realm.where(LapEntity::class.java).count())
                }
            } finally { instrumentation.runOnMainSync { source?.close() } }
        }
    }

    @Test fun changedDeliveryRetriesFirstImportFailureAndFirstAckPutFailure() {
        withIsolatedRealm { config ->
            var imports = 0
            var ackPuts = 0
            var storedAck: com.google.android.gms.wearable.PutDataRequest? = null
            val encoded = ActivitySyncCodec.encode(record())
            val envelopes = mutableListOf<ByteArray>()
            repeat(3) { attempt ->
                val request = com.google.android.gms.wearable.PutDataMapRequest.create("/activities/${record().activityId}").apply {
                    dataMap.putString("deliveryAttempt", "attempt-$attempt")
                    dataMap.putString("activityId", record().activityId)
                    dataMap.putInt("schemaVersion", 1)
                    dataMap.putByteArray("payload", encoded)
                }.asPutDataRequest()
                envelopes += request.data!!
                val dataItem = object : com.google.android.gms.wearable.DataItem {
                    override fun getUri() = request.uri
                    override fun getData() = request.data
                    override fun setData(data: ByteArray?): com.google.android.gms.wearable.DataItem = error("read only")
                    override fun getAssets() = emptyMap<String, com.google.android.gms.wearable.DataItemAsset>()
                    override fun freeze(): com.google.android.gms.wearable.DataItem = this
                    override fun isDataValid() = true
                }
                val incoming = ActivitySyncDataItem.read(dataItem) { error("inline payload") }
                assertArrayEquals(encoded, incoming.payload)
                val done = CountDownLatch(1)
                var failure: Throwable? = null
                instrumentation.runOnMainSync {
                    ActivitySyncIngestion(
                        { r, success, rejected ->
                            imports++
                            if (imports == 1) rejected(IllegalStateException("transient importer failure"))
                            else ActivityRepository(ActivityDao()).importSynced(r, success, rejected)
                        },
                        { id, version ->
                            // Same asynchronous Task failure shape as a failed DataClient put.
                            ackPuts++
                            val put = activityAckRequest(id, version)
                            val result = if (ackPuts == 1) com.google.android.gms.tasks.Tasks.forException<Unit>(IllegalStateException("ACK put failed"))
                                else com.google.android.gms.tasks.Tasks.forResult(Unit)
                            result.addOnSuccessListener { storedAck = put; done.countDown() }
                                .addOnFailureListener { failure = it; done.countDown() }
                        },
                        { failure = it; done.countDown() },
                    ).receive(incoming.path, incoming.activityId, incoming.schemaVersion, incoming.payload)
                }
                assertTrue(done.await(10, TimeUnit.SECONDS))
                assertEquals(attempt < 2, failure != null)
                assertEquals(attempt == 2, storedAck != null)
                Realm.getInstance(config).use { realm ->
                    assertEquals(if (attempt == 0) 0L else 1L, realm.where(ActivityEntity::class.java).count())
                }
            }
            assertFalse(envelopes[0].contentEquals(envelopes[1]))
            assertFalse(envelopes[1].contentEquals(envelopes[2]))
            assertEquals(3, imports)
            assertEquals(2, ackPuts)
            assertEquals("/activity-acks/${record().activityId}", storedAck!!.uri.path)
            Realm.getInstance(config).use { realm ->
                val activity = realm.where(ActivityEntity::class.java).findAll().single()
                assertEquals(record().activityId, activity.id)
                assertEquals(record().name, activity.name)
                assertEquals(listOf(4000L, 3000L), activity.chronometer!!.laps.map { it.runningTime })
                assertEquals(1L, realm.where(ChronometerEntity::class.java).count())
                assertEquals(2L, realm.where(LapEntity::class.java).count())
            }
        }
    }

    private fun withIsolatedRealm(test: (RealmConfiguration) -> Unit) {
        Realm.init(instrumentation.targetContext)
        val previous = Realm.getDefaultConfiguration()!!
        val config = RealmConfiguration.Builder().name("sync-test-${System.nanoTime()}.realm").build()
        Realm.setDefaultConfiguration(config)
        try { test(config) } finally {
            Realm.setDefaultConfiguration(previous)
            // The ACK releases the test thread while Realm's async completion can still
            // be unwinding on the main/worker threads. Do not race file deletion with it.
            instrumentation.waitForIdleSync()
            val deadline = android.os.SystemClock.elapsedRealtime() + 5_000
            while (Realm.getGlobalInstanceCount(config) != 0 && android.os.SystemClock.elapsedRealtime() < deadline) {
                android.os.SystemClock.sleep(10)
            }
            assertEquals("Isolated Realm instances must be closed before cleanup", 0, Realm.getGlobalInstanceCount(config))
            Realm.deleteRealm(config)
        }
    }
}
