package br.com.helpdev.lapscounter.wear.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.helpdev.lapscounter.wear.timer.ElapsedRealtimeClock
import br.com.helpdev.lapscounter.wear.timer.WatchSessionState
import br.com.helpdev.lapscounter.wear.timer.WatchTimerController
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WatchActivityRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "watch-repository-test.db"
    private lateinit var database: WatchDatabase
    private lateinit var repository: WatchActivityRepository
    private var now = 1_000L

    @Before fun setUp() {
        context.deleteDatabase(databaseName)
        reopen()
    }

    @After fun tearDown() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    private fun reopen() {
        database = Room.databaseBuilder(context, WatchDatabase::class.java, databaseName).build()
        repository = WatchActivityRepository(database)
    }

    @Test fun reopensActiveSnapshotSettingsAndCompletedHistoryWithoutLosingLaps() = runBlocking {
        val timer = WatchTimerController(ElapsedRealtimeClock { now }, "swimming", 50f, false) { 123_456L }
        repository.saveSettings(WearSettingsEntity("swimming", 50f, false))
        repository.persistActive(timer.start())
        now = 4_000
        repository.persistActive(timer.recordLap())
        now = 5_000
        val paused = timer.pause()
        repository.persistActive(paused)
        database.close()
        reopen()

        assertEquals(paused, repository.loadActive())
        assertEquals(WearSettingsEntity("swimming", 50f, false), repository.settings("swimming"))
        assertTrue(repository.history().isEmpty())

        now = 10_000
        val restored = WatchTimerController(ElapsedRealtimeClock { now }, "swimming", 50f, false, repository.loadActive())
        repository.persistActive(restored.resume())
        now = 12_000
        val stopped = restored.stop()
        repository.persistActive(stopped)
        database.close()
        reopen()
        assertEquals(stopped, repository.loadActive())
        repository.saveCompleted(stopped, "Morning swimming", "Pool")
        database.close()
        reopen()

        assertNull(repository.loadActive())
        val record = repository.history().single()
        assertEquals("Morning swimming", record.activity.name)
        assertEquals("Pool", record.activity.description)
        assertEquals(stopped.id, record.activity.id)
        assertEquals(WatchSyncStatus.PENDING, record.activity.syncStatus)
        assertEquals(6_000L, record.activity.elapsedMillis)
        assertEquals(5_000L, record.activity.accumulatedPausedMillis)
        assertEquals(listOf(1, 2), record.laps.map { it.sequence })
        assertEquals(listOf(3_000L, 3_000L), record.laps.map { it.durationMillis })
        assertEquals(stopped, record.toSnapshot())
        assertEquals(record, repository.activity(stopped.id))
        repository.saveCompleted(stopped, "Morning swimming", "Pool")
        assertEquals(1, repository.history().size)
    }

    @Test fun acknowledgementRetainsRecordAndOnlyUpdatesMatchingFlavor() = runBlocking {
        val timer = WatchTimerController(ElapsedRealtimeClock { now }, "swimming", 50f)
        timer.start()
        now = 2_000
        val completed = timer.stop()
        repository.saveCompleted(completed, "Saved")
        assertEquals(listOf(completed.id), repository.pendingActivities("swimming").map { it.activity.id })
        assertTrue(repository.pendingActivities("running").isEmpty())
        repository.markSynced(completed.id, "running")
        assertEquals(WatchSyncStatus.PENDING, repository.activity(completed.id)!!.activity.syncStatus)
        repository.markSynced(completed.id, "swimming")
        assertTrue(repository.pendingActivities("swimming").isEmpty())
        repository.saveCompleted(completed, "Retry")
        database.close()
        reopen()
        assertEquals(WatchSyncStatus.SYNCED, repository.activity(completed.id)!!.activity.syncStatus)
        assertEquals("Saved", repository.activity(completed.id)!!.activity.name)
        assertEquals(1, repository.activity(completed.id)!!.laps.size)
    }

    @Test fun discardingActiveSessionPreservesCompletedHistory() = runBlocking {
        val first = WatchTimerController(ElapsedRealtimeClock { now }, "swimming", 50f)
        first.start()
        now = 2_000
        val completed = first.stop()
        repository.saveCompleted(completed, "Saved")
        val second = WatchTimerController(ElapsedRealtimeClock { now }, "swimming", 50f)
        repository.persistActive(second.start())
        now = 3_000
        repository.persistActive(second.recordLap())
        repository.persistActive(second.discard())
        database.close()
        reopen()
        assertNull(repository.loadActive())
        assertEquals(listOf(completed.id), repository.history().map { it.activity.id })
        assertEquals(1, repository.history().single().laps.size)
        assertNull(repository.activity(second.snapshot().id))
    }

    @Test fun readingDisplayTicksDoesNotPersistThem() = runBlocking {
        val timer = WatchTimerController(ElapsedRealtimeClock { now }, "swimming", 50f)
        val started = timer.start()
        repository.persistActive(started)
        now = 9_000
        assertEquals(8_000L, timer.snapshot().elapsedMillis)
        assertEquals(started, repository.loadActive())
        assertEquals(WatchSessionState.RUNNING, repository.loadActive()!!.state)
    }
}
