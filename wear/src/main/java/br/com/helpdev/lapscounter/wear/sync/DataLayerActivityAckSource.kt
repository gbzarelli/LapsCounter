package br.com.helpdev.lapscounter.wear.sync

import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataItemBuffer
import com.google.android.gms.wearable.DataMapItem
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException

/** Only decoding is fallible here; persistence errors are handled by the coordinator. */
internal fun decodeActivityAcknowledgement(item: DataItem): ActivityAcknowledgement? = try {
    val map = DataMapItem.fromDataItem(item).dataMap
    ActivityAcknowledgement(item.uri.path, map.getString("activityId"), map.getInt("schemaVersion", -1))
} catch (_: RuntimeException) {
    null
}

class DataLayerActivityAckSource internal constructor(
    private val query: (Uri, Int) -> Task<DataItemBuffer>,
) : ActivityAckSource {
    constructor(client: DataClient) : this(client::getDataItems)

    override suspend fun acknowledgements(): List<ActivityAcknowledgement> = suspendCancellableCoroutine { continuation ->
        query(Uri.parse("wear://*/activity-acks/"), DataClient.FILTER_PREFIX)
            .addOnSuccessListener { buffer ->
                // Copy metadata and release before resuming, including late results after cancellation.
                val result = try {
                    Result.success(if (continuation.isActive) buffer.mapNotNull(::decodeActivityAcknowledgement) else emptyList())
                } catch (error: Exception) {
                    Result.failure(error)
                } finally {
                    buffer.release()
                }
                if (continuation.isActive) continuation.resumeWith(result)
            }
            .addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
            .addOnCanceledListener { continuation.cancel() }
    }
}
