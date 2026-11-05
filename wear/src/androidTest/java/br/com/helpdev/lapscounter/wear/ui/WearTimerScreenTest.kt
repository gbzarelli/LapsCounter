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

@RunWith(AndroidJUnit4::class)
class WearTimerScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: WatchDatabase
    private lateinit var repository: WatchActivityRepository
    private lateinit var model: WearViewModel
    private val store = ViewModelStore()
    private var now = 1_000L

    @Before fun setUp() {
        compose.activityRule.scenario.onActivity {
            it.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        database = Room.inMemoryDatabaseBuilder(context, WatchDatabase::class.java).build()
        repository = WatchActivityRepository(database)
        model = WearViewModel(repository, context.resources, ElapsedRealtimeClock { now }, { 1_735_732_800_000L })
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

    @Test fun timerActionsFollowStateAndSummarySavesGeneratedNameAndSplits() {
        action(R.string.action_start).assertIsDisplayed()
        action(R.string.action_pause).assertDoesNotExist()
        action(R.string.action_start).performClick()
        compose.waitForIdle()
        waitForState(br.com.helpdev.lapscounter.wear.timer.WatchSessionState.RUNNING)
        assertFullyInViewport(R.string.action_lap)
        assertFullyInViewport(R.string.action_pause)
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
        val displayedName = model.state.value.generatedName
        click(R.string.action_save)
        compose.waitUntil(10_000) { model.state.value.history.size == 1 }
        val record = runBlocking { repository.history().single() }
        assertEquals(displayedName, record.activity.name)
        assertTrue(record.activity.name.startsWith(context.getString(R.string.sport_name)))
        assertTrue(record.activity.name.contains("2025"))
        assertEquals(listOf(60_000L, 0L), record.laps.map { it.durationMillis })
        assertEquals(WatchSyncStatus.PENDING, record.activity.syncStatus)
        assertNull(runBlocking { repository.loadActive() })
        click(R.string.history_title)
        reveal(record.activity.name).performClick()
        reveal(context.getString(R.string.lap_number, 1)).assertIsDisplayed()
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
