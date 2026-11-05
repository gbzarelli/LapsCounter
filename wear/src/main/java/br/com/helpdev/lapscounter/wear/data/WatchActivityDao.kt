package br.com.helpdev.lapscounter.wear.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface WatchActivityDao {
    @Upsert suspend fun upsertActivity(activity: WatchActivityEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertLaps(laps: List<WatchLapEntity>)
    @Query("DELETE FROM watch_laps WHERE activityId = :id") suspend fun deleteLaps(id: String)
    @Query("DELETE FROM watch_activities WHERE id = :id AND isActive = 1") suspend fun deleteActive(id: String)
    @Query("SELECT * FROM watch_activities WHERE isActive = 1 LIMIT 1") suspend fun active(): WatchActivityEntity?
    @Query("SELECT * FROM watch_activities WHERE id = :id") suspend fun activity(id: String): WatchActivityEntity?
    @Query("SELECT * FROM watch_laps WHERE activityId = :id ORDER BY sequence") suspend fun laps(id: String): List<WatchLapEntity>
    @Query("SELECT * FROM watch_activities WHERE isActive = 0 ORDER BY startWallClockMillis DESC, id")
    suspend fun history(): List<WatchActivityEntity>
    @Upsert suspend fun upsertSettings(settings: WearSettingsEntity)
    @Query("SELECT * FROM wear_settings WHERE flavorId = :flavorId") suspend fun settings(flavorId: String): WearSettingsEntity?
}
