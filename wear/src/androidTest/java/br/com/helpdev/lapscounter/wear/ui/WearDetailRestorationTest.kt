package br.com.helpdev.lapscounter.wear.ui

import android.content.Context
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.compose.material3.MaterialTheme
import br.com.helpdev.lapscounter.wear.R
import br.com.helpdev.lapscounter.wear.data.WatchActivityRepository
import br.com.helpdev.lapscounter.wear.data.WatchDatabase
import br.com.helpdev.lapscounter.wear.data.WatchSyncStatus
import br.com.helpdev.lapscounter.wear.timer.ElapsedRealtimeClock
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WearDetailRestorationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val store = ViewModelStore()
    private lateinit var database: WatchDatabase

    @After fun cleanup() { store.clear(); database.close() }

    @Test fun restoredDetailRouteLoadsSelectedRecordIntoNewViewModel() {
        compose.activityRule.scenario.onActivity { it.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        database = Room.inMemoryDatabaseBuilder(context, WatchDatabase::class.java).build()
        val repository = WatchActivityRepository(database)
        val first = WearViewModel(repository, context.resources, ElapsedRealtimeClock { 1000L }, { 1_735_732_800_000L })
        store.put("first", first)
        val current = mutableStateOf(first)
        val restoration = StateRestorationTester(compose)
        restoration.setContent { MaterialTheme { WearApp(current.value) } }
        compose.waitUntil(10_000) { first.state.value.ready }
        fun click(id: Int) {
            val text = context.getString(id)
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(text))
            compose.onNodeWithText(text).performClick()
            compose.waitForIdle()
            compose.waitUntil(10_000) { !first.state.value.busy }
        }
        click(R.string.action_start)
        click(R.string.action_pause)
        click(R.string.action_stop)
        click(R.string.action_save)
        val record = runBlocking { repository.history().single() }
        click(R.string.history_title)
        compose.waitUntil(10_000) { first.state.value.history.size == 1 }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(record.activity.name))
        compose.onNodeWithText(record.activity.name).performClick()
        compose.waitUntil(10_000) { first.state.value.detail?.activity?.id == record.activity.id }
        val second = WearViewModel(repository, context.resources, ElapsedRealtimeClock { 1000L })
        store.put("second", second)
        compose.runOnIdle { current.value = second }
        restoration.emulateSavedInstanceStateRestore()
        compose.waitUntil(10_000) { second.state.value.detail?.activity?.id == record.activity.id }
        compose.onNodeWithText(record.activity.name).assertIsDisplayed()
    }

    @Test fun saveAndResumeRetryDurablePendingRecordsAndRevisitingScreensRefreshesAck() {
        compose.activityRule.scenario.onActivity { it.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        database = Room.inMemoryDatabaseBuilder(context, WatchDatabase::class.java).build()
        val repository = WatchActivityRepository(database)
        val requestedIds = java.util.concurrent.CopyOnWriteArrayList<String>()
        val model = WearViewModel(
            repository, context.resources, ElapsedRealtimeClock { 1000L }, { 1_735_732_800_000L },
            retrySync = { requestedIds.addAll(repository.history().map { it.activity.id }) },
        )
        store.put("sync", model)
        compose.setContent { MaterialTheme { WearApp(model) } }
        compose.waitUntil(10_000) { model.state.value.ready }
        fun click(id: Int) {
            val text = context.getString(id)
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(text))
            compose.onNodeWithText(text).performClick()
            compose.waitForIdle()
            compose.waitUntil(10_000) { !model.state.value.busy }
        }
        click(R.string.action_start)
        click(R.string.action_pause)
        click(R.string.action_stop)
        click(R.string.action_save)
        compose.waitUntil(10_000) { requestedIds.size == 1 }
        val record = runBlocking { repository.history().single() }
        assertEquals(record.activity.id, requestedIds.single())
        assertEquals(WatchSyncStatus.PENDING, record.activity.syncStatus)
        compose.runOnIdle { model.onAppResumed() }
        compose.waitUntil(10_000) { requestedIds.size == 2 }
        assertEquals(record.activity.id, requestedIds.last())
        click(R.string.history_title)
        compose.waitUntil(10_000) { model.state.value.history.size == 1 }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(record.activity.name))
        compose.onNodeWithText(record.activity.name).performClick()
        compose.waitUntil(10_000) { model.state.value.detail?.activity?.id == record.activity.id }
        runBlocking { repository.markSynced(record.activity.id, record.activity.flavorId) }
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitUntil(10_000) { model.state.value.history.single().activity.syncStatus == WatchSyncStatus.SYNCED }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(record.activity.name))
        compose.onNodeWithText(record.activity.name).performClick()
        compose.waitUntil(10_000) { model.state.value.detail?.activity?.syncStatus == WatchSyncStatus.SYNCED }
    }


    @Test fun appResumeRefreshesHistoryAndDetailAfterAckReconciliation() {
        database = Room.inMemoryDatabaseBuilder(context, WatchDatabase::class.java).build()
        val repository = WatchActivityRepository(database)
        val timer = br.com.helpdev.lapscounter.wear.timer.WatchTimerController(ElapsedRealtimeClock { 1000 }, "swimming", 25f)
        timer.start()
        val snapshot = timer.stop()
        runBlocking { repository.saveCompleted(snapshot, "Saved") }
        val model = WearViewModel(repository, context.resources,
            retrySync = { repository.markSynced(snapshot.id, "swimming") })
        store.put("resume-sync", model)
        compose.setContent { MaterialTheme { WearApp(model) } }
        compose.waitUntil(10_000) { model.state.value.ready }
        compose.runOnIdle {
            model.loadHistory()
            model.loadActivity(snapshot.id)
        }
        compose.waitUntil(10_000) { model.state.value.history.size == 1 && model.state.value.detail != null }
        assertEquals(WatchSyncStatus.PENDING, model.state.value.history.single().activity.syncStatus)
        compose.runOnIdle { model.onAppResumed() }
        compose.waitUntil(10_000) {
            model.state.value.history.single().activity.syncStatus == WatchSyncStatus.SYNCED &&
                model.state.value.detail?.activity?.syncStatus == WatchSyncStatus.SYNCED
        }
    }

}
