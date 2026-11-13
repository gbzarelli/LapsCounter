package br.com.helpdev.lapscounter.model.repository

import br.com.helpdev.lapscounter.model.RealmLiveData
import br.com.helpdev.lapscounter.model.dao.ActivityDao
import br.com.helpdev.lapscounter.model.entity.ActivityEntity
import br.com.helpdev.lapscounter.sport.SportProfileProvider
import br.com.helpdev.lapscounter.sync.ActivitySyncImporter
import br.com.helpdev.lapscounter.sync.ActivitySyncRecord

class ActivityRepository(private val activityDao: ActivityDao) {

    companion object {
        @Volatile
        private var instance: ActivityRepository? = null

        fun getInstance(activityDao: ActivityDao) = instance ?: synchronized(this) {
            instance ?: ActivityRepository(activityDao).also { instance = it }
        }
    }

    fun save(activityEntity: ActivityEntity) {
        activityDao.insertOrUpdate(activityEntity)
    }

    fun importSynced(record: ActivitySyncRecord, onCommitted: () -> Unit, onError: (Throwable) -> Unit) {
        val entity = try {
            ActivitySyncImporter.toActivityEntity(record, SportProfileProvider.get().sportId.name.lowercase(java.util.Locale.ROOT))
        } catch (error: Exception) {
            onError(error)
            return
        }
        activityDao.importSynced(entity, onCommitted, onError)
    }

    fun getAllActivities(): RealmLiveData<ActivityEntity> {
        return activityDao.selectAll()
    }

    fun delete(activityEntity: ActivityEntity) {
        activityDao.delete(activityEntity)
    }

    fun getActivity(activityEntityId: String): ActivityEntity {
        return activityDao.select(activityEntityId)
    }
}