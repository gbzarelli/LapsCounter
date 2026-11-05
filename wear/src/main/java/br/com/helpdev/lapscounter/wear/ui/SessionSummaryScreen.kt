package br.com.helpdev.lapscounter.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import br.com.helpdev.lapscounter.wear.R

@Composable
fun SessionSummaryScreen(state: WearUiState, onSave: () -> Unit, onDiscard: () -> Unit) {
    WearList {
        item { WearText(stringResource(R.string.summary_title)) }
        item { WearAction(stringResource(R.string.action_save), !state.busy, onSave) }
        item { WearAction(stringResource(R.string.action_discard), !state.busy, onDiscard) }
        item { WearText(state.generatedName) }
        item { WearText(elapsedLabel(state.snapshot?.elapsedMillis ?: 0)) }
        item { WearText(state.metrics?.travelledDistance.orEmpty()) }
        item { WearText(state.metrics?.metric.orEmpty()) }
        item { WearText(stringResource(R.string.laps_total, state.snapshot?.laps?.size ?: 0)) }
        item { StorageError(state.error) }
    }
}
