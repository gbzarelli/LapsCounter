package br.com.helpdev.lapscounter.wear.sync

import android.util.Log
import br.com.helpdev.lapscounter.wear.data.WatchActivityRepository
import br.com.helpdev.lapscounter.wear.data.WatchDatabase
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.runBlocking

open class ActivityAckListenerService : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        runSync { coordinator ->
            for (event in events) {
                if (event.type != DataEvent.TYPE_CHANGED) continue
                val ack = decodeActivityAcknowledgement(event.dataItem) ?: continue
                coordinator.receive(ack)
            }
        }
    }

    override fun onPeerConnected(peer: Node) {
        runSync { it.recover() }
    }

    override fun onConnectedNodes(connectedNodes: MutableList<Node>) {
        if (connectedNodes.isNotEmpty()) runSync { it.recover() }
    }

    /** WearableListenerService dispatches callbacks on its background handler thread. */
    protected open fun runSync(action: suspend (ActivitySyncCoordinator) -> Unit) {
        val database = WatchDatabase.open(applicationContext)
        try {
            val coordinator = createActivitySyncCoordinator(applicationContext, WatchActivityRepository(database))
            runBlocking { action(coordinator) }
        } catch (error: Exception) {
            Log.w("ActivitySync", "Background sync failed; retry on connection or app resume", error)
        } finally {
            database.close()
        }
    }
}
