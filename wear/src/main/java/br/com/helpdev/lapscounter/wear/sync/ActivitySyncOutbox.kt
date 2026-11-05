package br.com.helpdev.lapscounter.wear.sync

import br.com.helpdev.lapscounter.sync.*
import br.com.helpdev.lapscounter.wear.data.WatchActivityStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class ActivitySyncOutbox(private val store: WatchActivityStore, private val transport: ActivitySyncTransport, private val flavorId: String) {
    private val mutex = Mutex()
    suspend fun retryPending() = mutex.withLock {
        for (record in store.pendingActivities(flavorId)) {
            try {
                val a = record.activity
                val snapshot = record.toSnapshot()
                transport.send(ActivitySyncRecord(ActivitySyncRecord.CURRENT_VERSION, a.flavorId, a.id, a.name, a.description,
                    requireNotNull(a.startWallClockMillis), a.lapDistanceMeters, snapshot.distanceMeters, a.countLastLap,
                    a.elapsedMillis, a.accumulatedPausedMillis, record.laps.sortedBy { it.sequence }.map {
                        ActivitySyncLap(it.sequence, it.durationMillis, it.elapsedMillis, it.isFinalPartialLap)
                    }))
            } catch (e: CancellationException) { throw e } catch (_: Exception) { /* Keep the durable pending row for retry. */ }
        }
    }
    suspend fun acknowledge(path: String?, version: Int) {
        if (version != ActivitySyncRecord.CURRENT_VERSION || path == null || !path.startsWith("/activity-acks/")) return
        val id = path.removePrefix("/activity-acks/")
        if (runCatching { UUID.fromString(id).toString() }.getOrNull() != id) return
        store.markSynced(id, flavorId)
    }
}
