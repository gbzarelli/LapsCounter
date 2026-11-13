package br.com.helpdev.lapscounter.model.dao

import br.com.helpdev.lapscounter.model.RealmLiveData
import br.com.helpdev.lapscounter.model.asLiveData
import br.com.helpdev.lapscounter.model.entity.ActivityEntity
import io.realm.Realm
import io.realm.Sort

class ActivityDao {

    companion object {
        @Volatile
        private var instance: ActivityDao? = null

        fun getInstance() = instance ?: synchronized(this) {
            instance ?: ActivityDao().also { instance = it }
        }
    }

    fun insertOrUpdate(activityEntity: ActivityEntity) {
        Realm.getDefaultInstance()
            .executeTransactionAsync { realm ->
                realm.insertOrUpdate(activityEntity)
            }
    }

    /** The caller must have a Looper; callbacks run on that same thread after commit/rollback. */
    fun importSynced(activityEntity: ActivityEntity, onCommitted: () -> Unit, onError: (Throwable) -> Unit) {
        val realm = try { Realm.getDefaultInstance() } catch (error: Exception) {
            onError(error)
            return
        }
        try {
            realm.executeTransactionAsync({ transaction ->
                // Nested Realm objects have no primary keys. Remove the replaced graph atomically
                // so retries do not accumulate orphan chronometers or laps.
                transaction.where(ActivityEntity::class.java).equalTo("id", activityEntity.id)
                    .findFirst()?.chronometer?.let { old ->
                        old.laps.deleteAllFromRealm()
                        old.deleteFromRealm()
                    }
                transaction.insertOrUpdate(activityEntity)
            }, {
                realm.close()
                onCommitted()
            }, { error ->
                realm.close()
                onError(error)
            })
        } catch (error: Exception) {
            realm.close()
            onError(error)
        }
    }

    fun selectAll(): RealmLiveData<ActivityEntity> {
        return Realm.getDefaultInstance()
            .where(ActivityEntity::class.java)
            .sort("dateStarted", Sort.DESCENDING)
            .findAllAsync()
            .asLiveData()
    }

    fun delete(activityEntity: ActivityEntity) {
        Realm.getDefaultInstance().use { db ->
            db.executeTransaction { realm ->
                realm.where(ActivityEntity::class.java)
                    .equalTo("id", activityEntity.id)
                    .findFirst()?.deleteFromRealm()
            }
        }
    }

    fun select(activityEntityId: String): ActivityEntity {
        return Realm.getDefaultInstance()
            .where(ActivityEntity::class.java)
            .equalTo("id", activityEntityId)
            .findFirst()!!
    }
}