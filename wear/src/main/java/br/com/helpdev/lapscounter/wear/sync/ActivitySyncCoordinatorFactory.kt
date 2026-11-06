package br.com.helpdev.lapscounter.wear.sync

import android.content.Context
import android.util.Log
import br.com.helpdev.lapscounter.wear.BuildConfig
import br.com.helpdev.lapscounter.wear.data.WatchActivityStore
import com.google.android.gms.wearable.Wearable

internal fun createActivitySyncCoordinator(context: Context, store: WatchActivityStore): ActivitySyncCoordinator {
    val client = Wearable.getDataClient(context.applicationContext)
    return ActivitySyncCoordinator(
        ActivitySyncOutbox(store, DataLayerActivitySyncTransport(PlayServicesDataClientAdapter(client)), BuildConfig.FLAVOR),
        DataLayerActivityAckSource(client),
        { Log.w("ActivitySync", "Sync recovery failed; pending activity and ACK remain available for retry", it) },
    )
}
