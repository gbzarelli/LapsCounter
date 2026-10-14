package br.com.helpdev.lapscounter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.com.helpdev.lapscounter.model.repository.ActivityRepository

class ActivityViewModelFactory(private val activityRepository: ActivityRepository) :
    ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChronometerViewModel::class.java))
            return ChronometerViewModel(activityRepository) as T
        if (modelClass.isAssignableFrom(ListActivityViewModel::class.java))
            return ListActivityViewModel(activityRepository) as T
        if (modelClass.isAssignableFrom(ActivityViewModel::class.java))
            return ActivityViewModel(activityRepository) as T
        throw IllegalArgumentException("${modelClass.name} is not accepted on this factory")
    }
}
