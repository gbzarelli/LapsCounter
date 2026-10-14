package br.com.helpdev.lapscounter.ui.fragment

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import br.com.helpdev.lapscounter.R
import br.com.helpdev.lapscounter.ui.ads.AdsManager

class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings_preference)
        configureLapDistance()
        configurePrivacyOptions()
    }

    private fun configureLapDistance() {
        val pref = getLapDistancePreference()
        pref.onPreferenceChangeListener = this
        onPreferenceChange(
            pref,
            PreferenceManager.getDefaultSharedPreferences(requireContext()).getFloat(
                pref.key,
                resources.getInteger(R.integer.pref_lap_distance_default_value).toFloat()
            )
        )
    }

    private fun getLapDistancePreference(): Preference =
        preferenceManager.findPreference(getString(R.string.pref_lap_distance_name))!!

    fun refreshPrivacyOptions() {
        preferenceManager.findPreference<Preference>(getString(R.string.pref_privacy_options_name))?.isVisible =
            AdsManager.privacyOptionsRequired()
    }

    private fun configurePrivacyOptions() {
        val preference = preferenceManager.findPreference<Preference>(getString(R.string.pref_privacy_options_name))
        preference?.isVisible = AdsManager.privacyOptionsRequired()
        preference?.setOnPreferenceClickListener {
            AdsManager.showPrivacyOptions(requireActivity()) { refreshPrivacyOptions() }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPrivacyOptions()
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        when (preference.key) {
            getString(R.string.pref_lap_distance_name) -> preference.summary =
                getString(R.string.pref_lap_distance_summary, newValue)
        }
        return true
    }

}
