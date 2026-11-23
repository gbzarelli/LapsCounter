package br.com.helpdev.lapscounter.wear.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.*
import androidx.wear.compose.navigation.*
import br.com.helpdev.lapscounter.wear.R
import br.com.helpdev.lapscounter.wear.timer.WatchSessionState

@Composable
fun WearApp(
    model: WearViewModel,
    onStart: () -> Unit = model::start,
    onNotificationSettings: () -> Unit = {},
) {
    val state by model.state.collectAsStateWithLifecycle()
    val navigation = rememberSwipeDismissableNavController()
    SwipeDismissableNavHost(navigation, startDestination = "timer") {
        composable("timer") {
            LifecycleResumeEffect(model) {
                model.setTimerVisible(true)
                onPauseOrDispose { model.setTimerVisible(false) }
            }
            if (!state.ready) {
                WearList { item { WearText(stringResource(if (state.error) R.string.storage_error else R.string.loading)) } }
            } else if (state.snapshot?.state == WatchSessionState.STOPPED) {
                SessionSummaryScreen(state, model::save, model::discard)
            } else {
                TimerScreen(state, onStart, model::lap, model::pause, model::resume, model::stop,
                    { navigation.navigate("history") }, { navigation.navigate("settings") },
                    model::finishAtCheckpoint, model::discard, onNotificationSettings)
            }
        }
        composable("history") {
            LaunchedEffect(model) { model.loadHistory() }
            ActivityHistoryScreen(state.history, state.historyLoading, state.historyError) {
                navigation.navigate("detail/${Uri.encode(it)}")
            }
        }
        composable("detail/{activityId}") { entry ->
            val id = requireNotNull(entry.arguments?.getString("activityId"))
            LaunchedEffect(id, model) { model.loadActivity(id) }
            ActivityDetailScreen(
                if (state.detailId == id) state.detail else null,
                state.detailId == id && state.detailError,
                model::metrics,
            )
        }
        composable("settings") {
            state.settings?.let { settings ->
                SettingsScreen(settings, state.busy, state.error) { distance, countLastLap ->
                    model.saveSettings(distance, countLastLap) { navigation.popBackStack() }
                }
            }
        }
    }
}

/** Extra inset and scrollable items keep every field reachable on the smallest round display. */
@Composable
internal fun WearList(content: ScalingLazyListScope.() -> Unit) {
    val state = rememberScalingLazyListState()
    val scrollIndicatorDescription = stringResource(R.string.scroll_indicator)
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        ScalingLazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 30.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content,
        )
        ScrollIndicator(
            state = state,
            modifier = Modifier.align(Alignment.CenterEnd).semantics {
                contentDescription = scrollIndicatorDescription
            },
        )
    }
}

@Composable
internal fun WearText(text: String) {
    Text(text, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
}

@Composable
internal fun WearNotice(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
internal fun WearAction(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(text, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

@Composable
internal fun StorageError(error: Boolean) {
    if (error) WearText(stringResource(R.string.storage_error))
}

internal fun elapsedLabel(millis: Long): String {
    val seconds = millis / 1000
    return "%02d:%02d:%02d".format(seconds / 3600, seconds / 60 % 60, seconds % 60)
}
