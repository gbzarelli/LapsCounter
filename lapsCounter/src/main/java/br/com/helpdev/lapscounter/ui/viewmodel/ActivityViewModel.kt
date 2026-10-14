package br.com.helpdev.lapscounter.ui.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.com.helpdev.lapscounter.R
import br.com.helpdev.lapscounter.model.entity.ActivityEntity
import br.com.helpdev.lapscounter.model.repository.ActivityRepository
import br.com.helpdev.lapscounter.sport.SportProfileProvider
import br.com.helpdev.lapscounter.ui.viewmodel.objects.HeaderDistances
import br.com.helpdev.lapscounter.utils.LapsCounterUtils
import br.com.helpdev.lapscounter.utils.presentSportMetrics
import br.com.helpdev.lapscounter.utils.sportMetricTitle

class ActivityViewModel(private val activityRepository: ActivityRepository) : ViewModel() {

    var activityEntity = MutableLiveData<ActivityEntity>()
    val headerDistances = HeaderDistances()

    fun init(context: Context, activityEntityId: String) {
        if (this.activityEntity.value != null) return
        //TODO - MAKE ASYNC
        getActivityEntity(activityEntityId).also { entity ->
            this.activityEntity.value = entity
            initHeaderDistances(context, entity)
        }
    }

    private fun getActivityEntity(activityEntityId: String): ActivityEntity {
        return activityRepository.getActivity(activityEntityId)
    }

    private fun initHeaderDistances(context: Context, activityEntity: ActivityEntity) {
        val profile = SportProfileProvider.get()
        val presentation = context.presentSportMetrics(
            runningTimeMillis = LapsCounterUtils.getRunningTime(activityEntity),
            splitDistanceMeters = activityEntity.lapDistance,
            travelledDistanceMeters = activityEntity.travelledDistance,
            profile = profile,
        )
        headerDistances.lapDistance.value =
            context.getString(R.string.info_distance_lap, presentation.splitDistance)
        headerDistances.travelledDistance.value =
            context.getString(R.string.info_travelled, presentation.travelledDistance)
        headerDistances.metric.value = context.getString(
            R.string.info_metric,
            context.sportMetricTitle(profile),
            presentation.metric,
        )
    }

    fun deleteActivity() {
        activityEntity.value?.let { activityRepository.delete(it) }
    }
}
