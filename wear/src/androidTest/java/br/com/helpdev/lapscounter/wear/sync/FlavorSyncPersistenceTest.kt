package br.com.helpdev.lapscounter.wear.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.helpdev.lapscounter.sport.SportProfileProvider
import br.com.helpdev.lapscounter.sync.ActivitySyncRecord
import br.com.helpdev.lapscounter.wear.data.WatchActivityRepository
import br.com.helpdev.lapscounter.wear.data.WatchDatabase
import br.com.helpdev.lapscounter.wear.data.WatchSyncStatus
import br.com.helpdev.lapscounter.wear.timer.ElapsedRealtimeClock
import br.com.helpdev.lapscounter.wear.timer.WatchTimerController
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/** Executed once per installed sport flavor. Real Room survives simulated process recreation. */
@RunWith(AndroidJUnit4::class)
class FlavorSyncPersistenceTest {
    @Test fun offlineSaveReopensPendingThenAckKeepsLocalUuidAndLaps() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        val name = "flavor-sync-${System.nanoTime()}.db"
        fun open() = Room.databaseBuilder(context, WatchDatabase::class.java, name).build()
        val profile = SportProfileProvider.get()
        val sport = profile.sportId.name.lowercase(Locale.ROOT)
        var now = 1_000L
        val timer = WatchTimerController(ElapsedRealtimeClock { now }, sport,
            profile.defaultLapDistanceMeters, wallClockMillis = { 1_700_000_000_000 })
        timer.start()
        now = 5_000
        timer.recordLap()
        now = 8_000
        val saved = timer.stop()
        var database = open()
        try {
            var repository = WatchActivityRepository(database)
            repository.saveCompleted(saved, "Offline $sport")
            ActivitySyncOutbox(repository, ActivitySyncTransport { error("Phone absent") }, sport).retryPending()
            assertEquals(WatchSyncStatus.PENDING, repository.activity(saved.id)!!.activity.syncStatus)
            database.close()
            database = open()
            repository = WatchActivityRepository(database)
            val sent = mutableListOf<ActivitySyncRecord>()
            val outbox = ActivitySyncOutbox(repository, ActivitySyncTransport { sent.add(it) }, sport)
            outbox.retryPending()
            val record = sent.single()
            assertEquals(saved.id, record.activityId)
            assertEquals(sport, record.sportId)
            assertEquals(listOf(4_000L, 3_000L), record.laps.map { it.durationMillis })
            assertEquals(WatchSyncStatus.PENDING, repository.activity(saved.id)!!.activity.syncStatus)
            outbox.acknowledge("/activity-acks/${saved.id}", 1)
            outbox.retryPending()
            assertEquals(1, sent.size)
            database.close()
            database = open()
            val history = WatchActivityRepository(database).history().single()
            assertEquals(saved.id, history.activity.id)
            assertEquals(WatchSyncStatus.SYNCED, history.activity.syncStatus)
            assertEquals(listOf(4_000L, 3_000L), history.laps.map { it.durationMillis })
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}
