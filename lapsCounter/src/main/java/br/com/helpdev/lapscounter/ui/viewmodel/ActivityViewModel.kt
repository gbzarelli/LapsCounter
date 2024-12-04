package br.com.helpdev.lapscounter.ui.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.com.helpdev.lapscounter.R
import br.com.helpdev.lapscounter.model.entity.ActivityEntity
import br.com.helpdev.lapscounter.model.repository.ActivityRepository
import br.com.helpdev.lapscounter.ui.viewmodel.objects.HeaderDistances
import br.com.helpdev.lapscounter.utils.LapsCounterUtils
import br.com.helpdev.lapscounter.utils.getStringLapDistance

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
        headerDistances.lapDistance.value = getStringLapDistance(context, activityEntity.lapDistance)
        headerDistances.travelledDistance.value = getStringTravelledDistance(context, activityEntity)
        headerDistances.pace.value = getStringPace(context, activityEntity)
    }

    private fun getStringTravelledDistance(context: Context, activityEntity: ActivityEntity) =
        context.getString(R.string.info_travelled, activityEntity.travelledDistance)

    private fun getStringPace(context: Context, activityEntity: ActivityEntity): String {
        val pace = LapsCounterUtils.getPace(activityEntity)
        return context.getString(R.string.info_pace, pace.first, pace.second)
    }

    fun deleteActivity() {
        activityEntity.value?.let { activityRepository.delete(it) }
    }
}