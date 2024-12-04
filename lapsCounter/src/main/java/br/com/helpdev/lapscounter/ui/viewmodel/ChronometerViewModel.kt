package br.com.helpdev.lapscounter.ui.viewmodel

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import br.com.helpdev.chronometerlib.Chronometer
import br.com.helpdev.chronometerlib.ChronometerManager
import br.com.helpdev.lapscounter.model.repository.ActivityRepository
import br.com.helpdev.lapscounter.utils.LapsCounterUtils
import br.com.helpdev.lapscounter.utils.toActivityEntity

class ChronometerViewModel(private val activityRepository: ActivityRepository) : ViewModel() {

    var chronometer = ChronometerManager(Chronometer()) {
        SystemClock.elapsedRealtime()
    }

    fun saveActivity(name: String, description: String, lapDistance: Float, countLastLap: Boolean) {
        chronometer.chronometer.toActivityEntity(name, description, lapDistance).also {
            it.countLastLap = countLastLap
            it.travelledDistance =
                LapsCounterUtils.getDistanceTravelled(
                    chronometer.chronometer,
                    lapDistance,
                    countLastLap
                )
            activityRepository.save(it)
        }
    }
}