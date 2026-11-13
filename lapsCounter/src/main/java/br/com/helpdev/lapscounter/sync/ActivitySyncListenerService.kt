package br.com.helpdev.lapscounter.sync

import android.os.Handler
import android.os.Looper
import android.util.Log
import br.com.helpdev.lapscounter.model.dao.ActivityDao
import br.com.helpdev.lapscounter.model.repository.ActivityRepository
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

internal data class ActivitySyncDataItem(val path: String, val activityId: String, val schemaVersion: Int, val payload: ByteArray) {
    companion object {
        fun read(item: DataItem, loadAsset: (Asset) -> ByteArray): ActivitySyncDataItem {
            val path = requireNotNull(item.uri.path)
            val map = DataMapItem.fromDataItem(item).dataMap
            val id = requireNotNull(map.getString("activityId"))
            val version = map.getInt("schemaVersion", -1)
            require(path == "/activities/$id" && version == ActivitySyncRecord.CURRENT_VERSION)
            val inline = map.getByteArray("payload")
            val asset = map.getAsset("payloadAsset")
            require((inline != null) != (asset != null)) { "Exactly one payload is required" }
            val payload = inline ?: loadAsset(requireNotNull(asset))
            require(payload.size <= ActivitySyncCodec.MAX_BYTES)
            return ActivitySyncDataItem(path, id, version, payload)
        }
    }
}

internal fun readActivityPayload(stream: InputStream): ByteArray {
    val result = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = stream.read(buffer)
        if (count < 0) return result.toByteArray()
        require(result.size() + count <= ActivitySyncCodec.MAX_BYTES) { "Asset exceeds activity payload limit" }
        result.write(buffer, 0, count)
    }
}

internal fun activityAckRequest(id: String, version: Int): PutDataRequest =
    PutDataMapRequest.create("/activity-acks/$id").apply {
        dataMap.putString("activityId", id)
        dataMap.putInt("schemaVersion", version)
    }.asPutDataRequest().setUrgent()

/** ACK is deliberately reachable only from the repository's transaction-success callback. */
internal class ActivitySyncIngestion(
    private val import: (ActivitySyncRecord, () -> Unit, (Throwable) -> Unit) -> Unit,
    private val acknowledge: (String, Int) -> Unit,
    private val onError: (Throwable) -> Unit
) {
    fun receive(path: String?, activityId: String?, schemaVersion: Int, payload: ByteArray) {
        val record = try {
            require(activityId != null && path == "/activities/$activityId")
            require(schemaVersion == ActivitySyncRecord.CURRENT_VERSION)
            ActivitySyncCodec.decode(payload).also {
                require(it.activityId == activityId && it.schemaVersion == schemaVersion)
            }
        } catch (error: Exception) {
            onError(error)
            return
        }
        import(record, { acknowledge(record.activityId, record.schemaVersion) }, onError)
    }
}

class ActivitySyncListenerService : WearableListenerService() {
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onDataChanged(events: DataEventBuffer) {
        val client = Wearable.getDataClient(this)
        for (event in events) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            val item = event.dataItem
            val path = item.uri.path ?: continue
            if (!path.startsWith("/activities/")) continue
            try {
                val incoming = ActivitySyncDataItem.read(item) { asset ->
                    // Copy the Asset on the listener's background thread before returning.
                    // The DataEventBuffer remains owned by the framework.
                    val pending = client.getFdForAsset(asset)
                    val response = try {
                        Tasks.await(pending, 30, TimeUnit.SECONDS)
                    } catch (error: Exception) {
                        // A response arriving after timeout still owns a file descriptor.
                        pending.addOnSuccessListener(Executor { it.run() }) { it.release() }
                        throw error
                    }
                    try {
                        requireNotNull(response.inputStream).use(::readActivityPayload)
                    } finally {
                        response.release()
                    }
                }
                mainHandler.post {
                    ActivitySyncIngestion(
                        { record, success, failure ->
                            ActivityRepository.getInstance(ActivityDao.getInstance()).importSynced(record, success, failure)
                        },
                        { activityId, schemaVersion ->
                            client.putDataItem(activityAckRequest(activityId, schemaVersion))
                                .addOnFailureListener { Log.e(TAG, "Cannot publish committed activity acknowledgement", it) }
                        },
                        { Log.e(TAG, "Activity import rejected or failed", it) }
                    ).receive(incoming.path, incoming.activityId, incoming.schemaVersion, incoming.payload)
                }
            } catch (error: Exception) {
                Log.e(TAG, "Invalid activity DataItem or unreadable payload", error)
            }
        }
    }

    companion object { private const val TAG = "ActivitySyncListener" }
}
