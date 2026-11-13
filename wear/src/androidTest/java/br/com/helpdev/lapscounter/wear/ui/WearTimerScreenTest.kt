package br.com.helpdev.lapscounter.wear.ui

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import android.view.WindowManager
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.compose.material3.MaterialTheme
import br.com.helpdev.lapscounter.sport.SportId
import br.com.helpdev.lapscounter.sport.SportProfileProvider
import br.com.helpdev.lapscounter.wear.R
import br.com.helpdev.lapscounter.wear.data.*
import br.com.helpdev.lapscounter.wear.timer.ElapsedRealtimeClock
import kotlinx.coroutines.runBlocking
import androidx.lifecycle.ViewModelStore
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.Locale
import java.io.File
import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry

@RunWith(AndroidJUnit4::class)
class WearTimerScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: WatchDatabase
    private lateinit var repository: WatchActivityRepository
    private lateinit var model: WearViewModel
    private lateinit var testStore: FailingHistoryStore
    private class FailingHistoryStore(private val delegate: WatchActivityStore) : WatchActivityStore by delegate {
        var failHistory = false
        var failDetail = false
        override suspend fun history(): List<WatchActivityRecord> {
            if (failHistory) error("history unavailable")
            return delegate.history()
        }
        override suspend fun activity(id: String): WatchActivityRecord? {
            if (failDetail) error("detail unavailable")
            return delegate.activity(id)
        }
    }
    private val store = ViewModelStore()
    private var now = 1_000L

    @Before fun setUp() {
        compose.activityRule.scenario.onActivity {
            it.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        database = Room.inMemoryDatabaseBuilder(context, WatchDatabase::class.java).build()
        repository = WatchActivityRepository(database)
        testStore = FailingHistoryStore(repository)
        model = WearViewModel(testStore, context.resources, ElapsedRealtimeClock { now }, { 1_735_732_800_000L })
        store.put("wear", model)
        compose.setContent { MaterialTheme { WearApp(model) } }
        compose.waitUntil(10_000) { model.state.value.ready }
    }

    @After fun tearDown() {
        compose.runOnIdle { store.clear() }
        database.close()
    }

    private fun action(id: Int) = compose.onNodeWithText(context.getString(id))
    private fun reveal(text: String): SemanticsNodeInteraction {
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(text))
        return compose.onNodeWithText(text)
    }
    private fun assertAction(id: Int) { reveal(context.getString(id)).assertIsDisplayed() }
    private fun assertFullyInViewport(id: Int) {
        val node = action(id).assertIsDisplayed().fetchSemanticsNode()
        val bounds = node.boundsInRoot
        val display = context.resources.displayMetrics
        assertTrue("Action extends beyond viewport: $bounds", bounds.left >= 0f && bounds.top >= 0f &&
            bounds.right <= display.widthPixels && bounds.bottom <= display.heightPixels)
    }
    private fun click(id: Int) {
        reveal(context.getString(id)).performClick()
        compose.waitForIdle()
        compose.waitUntil(10_000) { !model.state.value.busy }
    }
    private fun waitForState(state: br.com.helpdev.lapscounter.wear.timer.WatchSessionState) {
        compose.waitUntil(10_000) { model.state.value.snapshot?.state == state && !model.state.value.busy }
    }

    private fun screenshot(stage: String) {
        val directory = File(context.getExternalFilesDir(null), "task7-qa").apply { mkdirs() }
        val name = "${SportProfileProvider.get().sportId}-${Locale.getDefault().toLanguageTag()}-$stage.png"
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(directory, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun timerActionsFollowStateAndSummarySavesGeneratedNameAndSplits() {
        action(R.string.action_start).assertIsDisplayed()
        action(R.string.action_pause).assertDoesNotExist()
        action(R.string.action_start).performClick()
        compose.waitForIdle()
        waitForState(br.com.helpdev.lapscounter.wear.timer.WatchSessionState.RUNNING)
        assertFullyInViewport(R.string.action_lap)
        assertFullyInViewport(R.string.action_pause)
        screenshot("running")
        assertAction(R.string.action_lap)
        assertAction(R.string.action_pause)
        action(R.string.action_start).assertDoesNotExist()
        now = 61_000L
        click(R.string.action_lap)
        click(R.string.action_pause)
        waitForState(br.com.helpdev.lapscounter.wear.timer.WatchSessionState.PAUSED)
        assertAction(R.string.action_resume)
        assertAction(R.string.action_stop)
        action(R.string.action_lap).assertDoesNotExist()
        click(R.string.action_resume)
        waitForState(br.com.helpdev.lapscounter.wear.timer.WatchSessionState.RUNNING)
        click(R.string.action_pause)
        waitForState(br.com.helpdev.lapscounter.wear.timer.WatchSessionState.PAUSED)
        click(R.string.action_stop)
        waitForState(br.com.helpdev.lapscounter.wear.timer.WatchSessionState.STOPPED)
        assertAction(R.string.action_save)
        assertAction(R.string.action_discard)
        screenshot("summary")
        reveal(model.state.value.metrics!!.metric).assertIsDisplayed()
        screenshot("summary-metric")
        val displayedName = model.state.value.generatedName
        click(R.string.action_save)
        val record = runBlocking { repository.history().single() }
        assertEquals(displayedName, record.activity.name)
        assertTrue(record.activity.name.startsWith(context.getString(R.string.sport_name)))
        assertTrue(record.activity.name.contains("2025"))
        assertEquals(listOf(60_000L, 0L), record.laps.map { it.durationMillis })
        assertEquals(WatchSyncStatus.PENDING, record.activity.syncStatus)
        assertNull(runBlocking { repository.loadActive() })
        click(R.string.history_title)
        compose.waitUntil(10_000) { model.state.value.history.size == 1 }
        reveal(record.activity.name).performClick()
        reveal(context.getString(R.string.sync_pending)).assertIsDisplayed()
        screenshot("detail-status")
        reveal(context.getString(R.string.lap_number, 1)).assertIsDisplayed()
        screenshot("detail")
        reveal(context.getString(R.string.final_lap)).assertIsDisplayed()
        screenshot("detail-laps")
    }

    @Test fun stoppedMetricUsesActiveSportProfileAndDiscardLeavesNoHistory() {
        click(R.string.action_start)
        waitForState(br.com.helpdev.lapscounter.wear.timer.WatchSessionState.RUNNING)
        now = 61_000L
        click(R.string.action_lap)
        click(R.string.action_pause)
        waitForState(br.com.helpdev.lapscounter.wear.timer.WatchSessionState.PAUSED)
        click(R.string.action_stop)
        waitForState(br.com.helpdev.lapscounter.wear.timer.WatchSessionState.STOPPED)
        val expected = when (SportProfileProvider.get().sportId) {
            SportId.SWIMMING -> "1:00 / 100 m"
            SportId.RUNNING -> "0:30 / km"
            SportId.CYCLING -> if (Locale.getDefault().language in listOf("pt", "es")) "600,0 km/h" else "600.0 km/h"
        }
        reveal(expected).assertIsDisplayed()
        click(R.string.action_discard)
        compose.waitUntil(10_000) { model.state.value.snapshot?.state == br.com.helpdev.lapscounter.wear.timer.WatchSessionState.IDLE }
        assertTrue(runBlocking { repository.history().isEmpty() })
        assertNull(runBlocking { repository.loadActive() })
    }


    @Test fun timerStartsWhenHistoryCannotBeRead() {
        testStore.failHistory = true
        val second = WearViewModel(testStore, context.resources, ElapsedRealtimeClock { now })
        store.put("second", second)
        compose.waitUntil(10_000) { second.state.value.ready }
        second.start()
        compose.waitUntil(10_000) { second.state.value.snapshot?.state == br.com.helpdev.lapscounter.wear.timer.WatchSessionState.RUNNING }
    }

    @Test fun saveClosesSummaryWhenHistoryIsUnavailable() {
        click(R.string.action_start)
        click(R.string.action_pause)
        click(R.string.action_stop)
        testStore.failHistory = true
        click(R.string.action_save)
        compose.waitUntil(10_000) { model.state.value.snapshot?.state == br.com.helpdev.lapscounter.wear.timer.WatchSessionState.IDLE }
        assertEquals(1, runBlocking { repository.history().size })
        assertNull(runBlocking { repository.loadActive() })
        action(R.string.action_save).assertDoesNotExist()
    }

    @Test fun missingOrUnreadableDetailShowsErrorInsteadOfPreviousActivity() {
        click(R.string.action_start)
        click(R.string.action_pause)
        click(R.string.action_stop)
        click(R.string.action_save)
        val record = runBlocking { repository.history().single() }
        model.loadActivity(record.activity.id)
        compose.waitUntil(10_000) { model.state.value.detail?.activity?.id == record.activity.id }
        model.loadActivity("missing")
        compose.waitUntil(10_000) { model.state.value.detailError }
        assertNull(model.state.value.detail)
        model.loadActivity(record.activity.id)
        compose.waitUntil(10_000) { model.state.value.detail?.activity?.id == record.activity.id }
        testStore.failDetail = true
        model.loadActivity("missing")
        compose.waitUntil(10_000) { model.state.value.detailError }
        assertNull(model.state.value.detail)
        click(R.string.history_title)
        compose.waitUntil(10_000) { model.state.value.history.size == 1 }
        reveal(record.activity.name).performClick()
        compose.waitUntil(10_000) { model.state.value.detailError }
        action(R.string.activity_read_error).assertIsDisplayed()
    }

    @Test fun historyReadFailureShowsErrorWhileTimerRemainsUsable() {
        testStore.failHistory = true
        click(R.string.history_title)
        compose.waitUntil(10_000) { model.state.value.historyError }
        action(R.string.history_read_error).assertIsDisplayed()
        assertTrue(model.state.value.ready)
    }

    @Test fun settingsPersistDistanceAndFinalLapChoiceAndApplyToNextSession() {
        assertTrue(model.state.value.settings!!.countLastLap)
        val defaultDistance = SportProfileProvider.get().defaultLapDistanceMeters
        assertEquals(defaultDistance, model.state.value.settings!!.lapDistanceMeters)
        click(R.string.settings_title)
        click(R.string.distance_increase)
        click(R.string.count_final_lap)
        click(R.string.action_save)
        compose.waitUntil(10_000) { model.state.value.settings?.countLastLap == false && !model.state.value.busy }
        val saved = runBlocking { repository.settings(model.flavorId)!! }
        assertTrue(saved.lapDistanceMeters > defaultDistance)
        assertFalse(saved.countLastLap)
        click(R.string.action_start)
        waitForState(br.com.helpdev.lapscounter.wear.timer.WatchSessionState.RUNNING)
        assertEquals(saved.lapDistanceMeters, runBlocking { repository.loadActive()!!.lapDistanceMeters })
        assertFalse(runBlocking { repository.loadActive()!!.countLastLap })
    }
}
