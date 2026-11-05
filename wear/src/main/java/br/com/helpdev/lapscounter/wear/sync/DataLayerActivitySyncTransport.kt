package br.com.helpdev.lapscounter.wear.sync

import br.com.helpdev.lapscounter.sync.ActivitySyncCodec
import br.com.helpdev.lapscounter.sync.ActivitySyncRecord
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

fun interface DataClientAdapter { suspend fun put(request: PutDataRequest) }

class PlayServicesDataClientAdapter(private val client: DataClient) : DataClientAdapter {
    override suspend fun put(request: PutDataRequest) = suspendCancellableCoroutine<Unit> { continuation ->
        client.putDataItem(request).addOnSuccessListener { if (continuation.isActive) continuation.resume(Unit) }
            .addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
            .addOnCanceledListener { continuation.cancel() }
    }
}

class DataLayerActivitySyncTransport(private val client: DataClientAdapter) : ActivitySyncTransport {
    override suspend fun send(record: ActivitySyncRecord) {
        val payload = ActivitySyncCodec.encode(record)
        val mapRequest = PutDataMapRequest.create("/activities/${record.activityId}")
        mapRequest.dataMap.putString("activityId", record.activityId)
        mapRequest.dataMap.putInt("schemaVersion", record.schemaVersion)
        if (payload.size <= 90 * 1024) mapRequest.dataMap.putByteArray("payload", payload)
        else mapRequest.dataMap.putAsset("payloadAsset", Asset.createFromBytes(payload))
        client.put(mapRequest.asPutDataRequest().setUrgent())
    }
}
