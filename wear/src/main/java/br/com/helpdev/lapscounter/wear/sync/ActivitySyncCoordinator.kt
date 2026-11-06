package br.com.helpdev.lapscounter.wear.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ActivityAcknowledgement(val path: String?, val activityId: String?, val schemaVersion: Int)

fun interface ActivityAckSource {
    suspend fun acknowledgements(): List<ActivityAcknowledgement>
}

/** Reconcile durable ACKs before sending. Each external trigger performs one bounded pass. */
class ActivitySyncCoordinator(
    private val outbox: ActivitySyncOutbox,
    private val ackSource: ActivityAckSource,
    private val onFailure: (Exception) -> Unit = {},
) {
    suspend fun recover() = recoveryMutex.withLock {
        val acknowledgements = try {
            ackSource.acknowledgements()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            onFailure(error)
            emptyList()
        }
        acknowledgements.forEach { applyAcknowledgement(it) }
        outbox.retryPending()
    }

    suspend fun receive(ack: ActivityAcknowledgement) = recoveryMutex.withLock {
        applyAcknowledgement(ack)
    }

    private suspend fun applyAcknowledgement(ack: ActivityAcknowledgement) {
        if (ack.path == null || ack.activityId == null ||
            ack.activityId != ack.path.removePrefix("/activity-acks/")) return
        try {
            outbox.acknowledge(ack.path, ack.schemaVersion)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            // The ACK remains in Data Layer for a later connection/resume reconciliation.
            onFailure(error)
        }
    }

    companion object {
        // Activity and background service can own separate Room connections/coordinators.
        private val recoveryMutex = Mutex()
    }
}
