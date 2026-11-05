package br.com.helpdev.lapscounter.wear.data

import androidx.room.withTransaction
import br.com.helpdev.lapscounter.wear.timer.WatchLapSplit
import br.com.helpdev.lapscounter.wear.timer.WatchSessionSnapshot
import br.com.helpdev.lapscounter.wear.timer.WatchSessionState

data class WatchActivityRecord(val activity: WatchActivityEntity, val laps: List<WatchLapEntity>) {
    fun toSnapshot() = WatchSessionSnapshot(
        id = activity.id,
        flavorId = activity.flavorId,
        startWallClockMillis = activity.startWallClockMillis,
        startElapsedRealtimeMillis = activity.startElapsedRealtimeMillis,
        pauseElapsedRealtimeMillis = activity.pauseElapsedRealtimeMillis,
        stopElapsedRealtimeMillis = activity.stopElapsedRealtimeMillis,
        accumulatedPausedMillis = activity.accumulatedPausedMillis,
        state = activity.state,
        lapDistanceMeters = activity.lapDistanceMeters,
        countLastLap = activity.countLastLap,
        laps = laps.sortedBy { it.sequence }.map { WatchLapSplit(it.sequence, it.durationMillis, it.elapsedMillis, it.isFinalPartialLap) },
        elapsedMillis = activity.elapsedMillis,
        syncStatus = activity.syncStatus,
    )
}

interface WatchActivityStore {
    suspend fun persistActive(snapshot: WatchSessionSnapshot)
    suspend fun loadActive(): WatchSessionSnapshot?
    suspend fun saveCompleted(snapshot: WatchSessionSnapshot, name: String, description: String? = null)
    suspend fun pendingActivities(flavorId: String): List<WatchActivityRecord>
    suspend fun markSynced(id: String, flavorId: String)
    suspend fun history(): List<WatchActivityRecord>
    suspend fun activity(id: String): WatchActivityRecord?
    suspend fun settings(flavorId: String): WearSettingsEntity?
    suspend fun saveSettings(settings: WearSettingsEntity)
}

/** Room is authoritative. Await transition writes; reading timer ticks never writes to disk. */
class WatchActivityRepository(private val database: WatchDatabase) : WatchActivityStore {
    private val dao = database.activityDao()

    override suspend fun persistActive(snapshot: WatchSessionSnapshot) = database.withTransaction {
        if (snapshot.state == WatchSessionState.DISCARDED || snapshot.state == WatchSessionState.IDLE) {
            dao.deleteActive(snapshot.id)
        } else {
            check(dao.active()?.id.let { it == null || it == snapshot.id }) { "Another session is active" }
            check(dao.activity(snapshot.id)?.isActive != false) { "Completed history cannot become active" }
            write(snapshot, isActive = true)
        }
    }

    override suspend fun loadActive(): WatchSessionSnapshot? = database.withTransaction {
        dao.active()?.let { WatchActivityRecord(it, dao.laps(it.id)).toSnapshot() }
    }

    override suspend fun saveCompleted(snapshot: WatchSessionSnapshot, name: String, description: String?) = database.withTransaction {
        require(snapshot.state == WatchSessionState.STOPPED) { "Stop the session before saving" }
        require(name.isNotBlank())
        val existing = dao.activity(snapshot.id)
        // Retried save must not revert an acknowledged sync status or rewrite immutable history.
        if (existing?.isActive != false) write(snapshot, isActive = false, name = name, description = description)
    }

    override suspend fun pendingActivities(flavorId: String): List<WatchActivityRecord> = database.withTransaction {
        dao.pendingActivities(flavorId).map { WatchActivityRecord(it, dao.laps(it.id)) }
    }

    override suspend fun markSynced(id: String, flavorId: String) { dao.markSynced(id, flavorId) }

    override suspend fun history(): List<WatchActivityRecord> = database.withTransaction {
        dao.history().map { WatchActivityRecord(it, dao.laps(it.id)) }
    }

    override suspend fun activity(id: String): WatchActivityRecord? = database.withTransaction {
        dao.activity(id)?.let { WatchActivityRecord(it, dao.laps(id)) }
    }

    override suspend fun settings(flavorId: String): WearSettingsEntity? = dao.settings(flavorId)

    override suspend fun saveSettings(settings: WearSettingsEntity) {
        require(settings.flavorId.isNotBlank())
        require(settings.lapDistanceMeters.isFinite() && settings.lapDistanceMeters > 0)
        dao.upsertSettings(settings)
    }

    private suspend fun write(snapshot: WatchSessionSnapshot, isActive: Boolean, name: String = "", description: String? = null) {
        dao.upsertActivity(WatchActivityEntity(
            id = snapshot.id,
            flavorId = snapshot.flavorId,
            startWallClockMillis = snapshot.startWallClockMillis,
            startElapsedRealtimeMillis = snapshot.startElapsedRealtimeMillis,
            pauseElapsedRealtimeMillis = snapshot.pauseElapsedRealtimeMillis,
            stopElapsedRealtimeMillis = snapshot.stopElapsedRealtimeMillis,
            accumulatedPausedMillis = snapshot.accumulatedPausedMillis,
            elapsedMillis = snapshot.elapsedMillis,
            state = snapshot.state,
            lapDistanceMeters = snapshot.lapDistanceMeters,
            countLastLap = snapshot.countLastLap,
            syncStatus = snapshot.syncStatus,
            isActive = isActive,
            name = name,
            description = description,
        ))
        dao.deleteLaps(snapshot.id)
        dao.insertLaps(snapshot.laps.map {
            WatchLapEntity(snapshot.id, it.sequence, it.durationMillis, it.elapsedMillis, it.isFinalPartialLap)
        })
    }
}
