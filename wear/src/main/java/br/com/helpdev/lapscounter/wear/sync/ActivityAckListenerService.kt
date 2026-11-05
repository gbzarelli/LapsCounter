package br.com.helpdev.lapscounter.wear.sync

import br.com.helpdev.lapscounter.wear.BuildConfig
import br.com.helpdev.lapscounter.wear.data.WatchActivityRepository
import br.com.helpdev.lapscounter.wear.data.WatchDatabase
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.runBlocking

class ActivityAckListenerService : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        val database = WatchDatabase.open(applicationContext)
        try {
            val outbox = ActivitySyncOutbox(WatchActivityRepository(database), ActivitySyncTransport { }, BuildConfig.FLAVOR)
            for (event in events) {
                if (event.type != DataEvent.TYPE_CHANGED) continue
                val item = event.dataItem
                try {
                    val map = DataMapItem.fromDataItem(item).dataMap
                    val path = item.uri.path
                    if (map.getString("activityId") != path?.removePrefix("/activity-acks/")) continue
                    runBlocking { outbox.acknowledge(path, map.getInt("schemaVersion", -1)) }
                } catch (_: RuntimeException) { /* Ignore malformed acknowledgement metadata. */ }
            }
        } finally { database.close() }
    }
}
