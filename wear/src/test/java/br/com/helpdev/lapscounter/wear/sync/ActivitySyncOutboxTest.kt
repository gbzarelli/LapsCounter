package br.com.helpdev.lapscounter.wear.sync

import br.com.helpdev.lapscounter.wear.data.*
import br.com.helpdev.lapscounter.wear.timer.*
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.CancellationException
import org.junit.Assert.*
import org.junit.Test

class ActivitySyncOutboxTest {
    private val id = "123e4567-e89b-12d3-a456-426614174000"
    private class Store(val record: WatchActivityRecord) : WatchActivityStore {
        var status = WatchSyncStatus.PENDING
        override suspend fun pendingActivities(flavorId: String) = if (flavorId == record.activity.flavorId && status == WatchSyncStatus.PENDING) listOf(record) else emptyList()
        override suspend fun markSynced(id: String, flavorId: String) { if (id == record.activity.id && flavorId == record.activity.flavorId) status = WatchSyncStatus.SYNCED }
        override suspend fun persistActive(snapshot: WatchSessionSnapshot) = Unit
        override suspend fun loadActive(): WatchSessionSnapshot? = null
        override suspend fun saveCompleted(snapshot: WatchSessionSnapshot, name: String, description: String?) = Unit
        override suspend fun history() = listOf(record)
        override suspend fun activity(id: String) = record.takeIf { it.activity.id == id }
        override suspend fun settings(flavorId: String): WearSettingsEntity? = null
        override suspend fun saveSettings(settings: WearSettingsEntity) = Unit
    }
    @Test fun offlineRetryPreservesPayloadAndOnlyMatchingAckMarksSynced() = runTest {
        val store = Store(WatchActivityRecord(WatchActivityEntity(id, "swimming", 100, 0, null, 100, 0, 100, WatchSessionState.STOPPED, 25f, true, isActive = false, name = "Swim"), listOf(WatchLapEntity(id, 1, 100, 100, true))))
        val sent = mutableListOf<br.com.helpdev.lapscounter.sync.ActivitySyncRecord>()
        var offline = true
        val outbox = ActivitySyncOutbox(store, ActivitySyncTransport { sent += it; if (offline) error("offline") }, "swimming")
        outbox.retryPending(); assertEquals(WatchSyncStatus.PENDING, store.status)
        offline = false; outbox.retryPending(); assertEquals(sent[0], sent[1]); assertEquals(id, sent[1].activityId)
        assertEquals(WatchSyncStatus.PENDING, store.status)
        outbox.acknowledge("/activities/$id", 1)
        outbox.acknowledge("/activity-acks/123e4567-e89b-12d3-a456-426614174001", 1)
        outbox.acknowledge("/activity-acks/$id/extra", 1)
        ActivitySyncOutbox(store, ActivitySyncTransport { }, "running").acknowledge("/activity-acks/$id", 1)
        assertEquals(WatchSyncStatus.PENDING, store.status)
        outbox.acknowledge("/activity-acks/$id", 2); outbox.acknowledge("/activity-acks/invalid", 1); assertEquals(WatchSyncStatus.PENDING, store.status)
        outbox.acknowledge("/activity-acks/$id", 1); assertEquals(WatchSyncStatus.SYNCED, store.status)
        outbox.acknowledge("/activity-acks/$id", 1)
        outbox.retryPending(); assertEquals(2, sent.size)
        assertEquals(id, store.history().single().activity.id)
    }

    @Test fun cancellationPropagatesWithoutAcknowledgingTheRecord() = runTest {
        val store = Store(WatchActivityRecord(
            WatchActivityEntity(id, "swimming", 100, 0, null, 100, 0, 100,
                WatchSessionState.STOPPED, 25f, true, isActive = false, name = "Swim"),
            emptyList(),
        ))
        val outbox = ActivitySyncOutbox(store, ActivitySyncTransport { throw CancellationException("cancelled") }, "swimming")
        try {
            outbox.retryPending()
            fail("Cancellation must propagate to the caller")
        } catch (_: CancellationException) {
            assertEquals(WatchSyncStatus.PENDING, store.status)
        }
    }

}
