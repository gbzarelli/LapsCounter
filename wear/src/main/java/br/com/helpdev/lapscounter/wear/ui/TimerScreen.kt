package br.com.helpdev.lapscounter.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import br.com.helpdev.lapscounter.wear.R
import br.com.helpdev.lapscounter.wear.timer.WatchSessionState

@Composable
fun TimerScreen(
    state: WearUiState,
    onStart: () -> Unit, onLap: () -> Unit, onPause: () -> Unit,
    onResume: () -> Unit, onStop: () -> Unit,
    onHistory: () -> Unit, onSettings: () -> Unit,
    onFinishAtCheckpoint: () -> Unit, onDiscard: () -> Unit,
) {
    val snapshot = state.snapshot ?: return
    WearList {
        if (snapshot.state == WatchSessionState.IDLE) {
            item { WearText(stringResource(R.string.sport_name)) }
            item { WearText(elapsedLabel(snapshot.elapsedMillis)) }
        } else {
            item { WearText(elapsedLabel(snapshot.elapsedMillis)) }
        }
        when (snapshot.state) {
            WatchSessionState.IDLE -> {
                item { WearAction(stringResource(R.string.action_start), !state.busy, onStart) }
                item { WearAction(stringResource(R.string.history_title), !state.busy, onHistory) }
                item { WearAction(stringResource(R.string.settings_title), !state.busy, onSettings) }
            }
            WatchSessionState.RUNNING -> {
                item { WearAction(stringResource(R.string.action_lap), !state.busy, onLap) }
                item { WearAction(stringResource(R.string.action_pause), !state.busy, onPause) }
            }
            WatchSessionState.PAUSED -> {
                item { WearAction(stringResource(R.string.action_resume), !state.busy, onResume) }
                item { WearAction(stringResource(R.string.action_stop), !state.busy, onStop) }
            }
            WatchSessionState.RECOVERY_REQUIRED -> {
                item { WearAction(stringResource(R.string.action_finish_checkpoint), !state.busy, onFinishAtCheckpoint) }
                item { WearAction(stringResource(R.string.action_discard), !state.busy, onDiscard) }
                item { WearNotice(stringResource(R.string.reboot_recovery)) }
            }
            else -> Unit
        }
        if (snapshot.state != WatchSessionState.IDLE) {
            item { WearText(stringResource(R.string.sport_name)) }
            item { WearText(stringResource(R.string.lap_number, snapshot.laps.size + 1)) }
            item { WearText(state.metrics?.travelledDistance.orEmpty()) }
            item { WearText(state.metrics?.metric.orEmpty()) }
        }
        item { StorageError(state.error) }
    }
}
