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

    private fun withIsolatedRealm(test: (RealmConfiguration) -> Unit) {
        Realm.init(instrumentation.targetContext)
        val previous = Realm.getDefaultConfiguration()!!
        val config = RealmConfiguration.Builder().name("sync-test-${System.nanoTime()}.realm").build()
        Realm.setDefaultConfiguration(config)
        try { test(config) } finally {
            Realm.setDefaultConfiguration(previous)
            Realm.deleteRealm(config)
        }
    }
}
