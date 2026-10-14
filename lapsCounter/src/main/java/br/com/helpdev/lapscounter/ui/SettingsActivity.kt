package br.com.helpdev.lapscounter.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import br.com.helpdev.lapscounter.R
import br.com.helpdev.lapscounter.databinding.ActivitySettingsBinding
import br.com.helpdev.lapscounter.ui.ads.AdsManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        AdsManager.requestConsent(this) { allowed ->
            if (allowed) loadAds()
            (supportFragmentManager.findFragmentById(R.id.fragment) as? br.com.helpdev.lapscounter.ui.fragment.SettingsFragment)
                ?.refreshPrivacyOptions()
        }
    }

    private fun loadAds() {
        AdsManager.loadBanner(binding.adView)
    }
}
