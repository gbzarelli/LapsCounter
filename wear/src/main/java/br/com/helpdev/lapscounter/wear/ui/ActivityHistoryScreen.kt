package br.com.helpdev.lapscounter.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.items
import br.com.helpdev.lapscounter.sport.SportMetricsPresentation
import br.com.helpdev.lapscounter.wear.R
import br.com.helpdev.lapscounter.wear.data.*
import br.com.helpdev.lapscounter.wear.timer.WatchSessionSnapshot

@Composable
fun ActivityHistoryScreen(history: List<WatchActivityRecord>, onSelect: (String) -> Unit) {
    WearList {
        item { WearText(stringResource(R.string.history_title)) }
        if (history.isEmpty()) item { WearText(stringResource(R.string.history_empty)) }
        items(history, key = { it.activity.id }) { record ->
            WearAction(record.activity.name) { onSelect(record.activity.id) }
        }
    }
}

@Composable
fun ActivityDetailScreen(record: WatchActivityRecord?, metrics: (WatchSessionSnapshot) -> SportMetricsPresentation) {
    WearList {
        if (record == null) {
            item { WearText(stringResource(R.string.loading)) }
        } else {
            val presentation = metrics(record.toSnapshot())
            item { WearText(record.activity.name) }
            item { WearText(elapsedLabel(record.activity.elapsedMillis)) }
            item { WearText(presentation.travelledDistance) }
            item { WearText(presentation.metric) }
            item { WearText(stringResource(when (record.activity.syncStatus) {
                WatchSyncStatus.PENDING -> R.string.sync_pending
                WatchSyncStatus.SYNCED -> R.string.sync_synced
                WatchSyncStatus.UNAVAILABLE -> R.string.sync_unavailable
            })) }
            items(record.laps, key = { it.sequence }) { lap ->
                Column {
                    WearText(stringResource(R.string.lap_number, lap.sequence))
                    WearText(elapsedLabel(lap.durationMillis))
                    if (lap.isFinalPartialLap) WearText(stringResource(R.string.final_lap))
                }
            }
        }
    }
}
