package br.com.helpdev.lapscounter.wear.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import br.com.helpdev.lapscounter.wear.timer.WatchSessionState

enum class WatchSyncStatus { PENDING, SYNCED, UNAVAILABLE }

@Entity(tableName = "watch_activities")
data class WatchActivityEntity(
    @PrimaryKey val id: String,
    val flavorId: String,
    val startWallClockMillis: Long?,
    val startElapsedRealtimeMillis: Long?,
    val pauseElapsedRealtimeMillis: Long?,
    val stopElapsedRealtimeMillis: Long?,
    val accumulatedPausedMillis: Long,
    val elapsedMillis: Long,
    val state: WatchSessionState,
    val lapDistanceMeters: Float,
    val countLastLap: Boolean,
    val syncStatus: WatchSyncStatus = WatchSyncStatus.PENDING,
    val isActive: Boolean = true,
    val name: String = "",
    val description: String? = null,
    val bootIdentity: String? = null,
    val checkpointRealtimeMillis: Long? = null,
)
