package br.com.helpdev.lapscounter.ui

import android.os.Bundle
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import br.com.helpdev.lapscounter.R
import br.com.helpdev.lapscounter.databinding.ActivityActivitiesBinding
import br.com.helpdev.lapscounter.ui.ads.InterstitialActivity

class ActivitiesActivity : InterstitialActivity() {

    private lateinit var binding: ActivityActivitiesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityActivitiesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val findNavController = Navigation.findNavController(this, R.id.nav_host_fragment)
        NavigationUI.setupActionBarWithNavController(this, findNavController)
    }

    override fun onSupportNavigateUp(): Boolean {
        return Navigation.findNavController(this, R.id.nav_host_fragment).navigateUp()
    }
}
