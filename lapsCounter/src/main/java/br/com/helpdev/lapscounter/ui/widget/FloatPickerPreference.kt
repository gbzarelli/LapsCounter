package br.com.helpdev.lapscounter.ui.widget

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import br.com.helpdev.lapscounter.sport.LapDistancePreferenceAdapter
import br.com.helpdev.lapscounter.sport.SportProfileProvider

class FloatPickerPreference(context: Context, attrs: AttributeSet?) : Preference(context, attrs) {
    private val preferenceContext = context

    override fun onClick() {
        super.onClick()
        val profile = SportProfileProvider.get()
        val storedMeters = getPersistedFloat(profile.defaultLapDistanceMeters)
        val displayValue = LapDistancePreferenceAdapter.toDisplayValue(
            storedMeters,
            profile.distanceDisplayUnit,
        )
        FloatPickerDialog(preferenceContext, displayValue).show { selectedDisplayValue ->
            val selectedMeters = runCatching {
                LapDistancePreferenceAdapter.toStoredMeters(selectedDisplayValue, profile.distanceDisplayUnit)
            }.getOrNull() ?: return@show

            if (callChangeListener(selectedMeters)) {
                persistFloat(selectedMeters)
            }
        }
    }

}
