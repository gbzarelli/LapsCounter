package br.com.helpdev.lapscounter.ui.widget

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import br.com.helpdev.lapscounter.R

class FloatPickerPreference(context: Context, attrs: AttributeSet?) : Preference(context, attrs) {
    private val preferenceContext = context

    override fun onClick() {
        super.onClick()
        FloatPickerDialog(
            preferenceContext,
            getPersistedFloat(preferenceContext.resources.getInteger(R.integer.pref_lap_distance_default_value).toFloat())
        )
            .show { value ->
                persistFloat(value)
                callChangeListener(value)
            }
    }

}
