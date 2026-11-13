package br.com.helpdev.lapscounter.wear.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Text
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import br.com.helpdev.lapscounter.wear.R
import br.com.helpdev.lapscounter.wear.data.WearSettingsEntity

@Composable
fun SettingsScreen(settings: WearSettingsEntity, busy: Boolean, error: Boolean, onSave: (Float, Boolean) -> Unit) {
    var distance by rememberSaveable(settings.flavorId) { mutableFloatStateOf(settings.lapDistanceMeters) }
    var countLastLap by rememberSaveable(settings.flavorId) { mutableStateOf(settings.countLastLap) }
    val step = if (settings.lapDistanceMeters < 100) 5f else 100f
    WearList {
        item { WearText(stringResource(R.string.settings_title)) }
        item { WearText(stringResource(R.string.lap_distance, distance)) }
        item { WearAction(stringResource(R.string.distance_increase), !busy) { distance += step } }
        item { WearAction(stringResource(R.string.distance_decrease), !busy && distance > step) { distance -= step } }
        item {
            Button(
                onClick = { countLastLap = !countLastLap }, enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
                    toggleableState = ToggleableState(countLastLap)
                    role = Role.Switch
                },
            ) { Text(stringResource(R.string.count_final_lap)) }
        }
        item { WearText(stringResource(if (countLastLap) R.string.setting_on else R.string.setting_off)) }
        item { WearAction(stringResource(R.string.action_save), !busy) { onSave(distance, countLastLap) } }
        item { StorageError(error) }
    }
}
