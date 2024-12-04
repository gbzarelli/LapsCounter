package br.com.helpdev.lapscounter.ui.fragment

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragment
import br.com.helpdev.lapscounter.R

class SettingsFragment : PreferenceFragment(), Preference.OnPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings_preference)
        configureLapDistance()
    }

    private fun configureLapDistance() {
        val pref = getLapDistancePreference()
        pref.onPreferenceChangeListener = this
        onPreferenceChange(
            pref,
            preferenceManager.sharedPreferences.getFloat(
                pref.key,
                resources.getInteger(R.integer.pref_lap_distance_default_value).toFloat()
            )
        )
    }

    private fun getLapDistancePreference(): Preference =
        preferenceManager.findPreference(getString(R.string.pref_lap_distance_name))!!

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        when (preference.key) {
            getString(R.string.pref_lap_distance_name) -> preference.summary =
                getString(R.string.pref_lap_distance_summary, newValue)
        }
        return true
    }

}