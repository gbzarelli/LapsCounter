package br.com.helpdev.lapscounter.sync

import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.wearable.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream

@RunWith(AndroidJUnit4::class)
class ActivitySyncDataItemTest {
    private val id = "123e4567-e89b-12d3-a456-426614174000"
    private fun request() = PutDataMapRequest.create("/activities/$id").apply {
        dataMap.putString("activityId", id)
        dataMap.putInt("schemaVersion", 1)
    }
    private fun item(request: PutDataRequest) = object : DataItem {
        override fun getUri() = request.uri
        override fun getData() = request.data
        override fun setData(data: ByteArray?): DataItem = error("read only")
        override fun getAssets() = request.assets.mapValues { (key, _) -> object : DataItemAsset {
            override fun getId() = "digest"
            override fun getDataItemKey() = key
            override fun freeze(): DataItemAsset = this
            override fun isDataValid() = true
        } }
        override fun freeze(): DataItem = this
        override fun isDataValid() = true
    }

    @Test fun readsInlineOrAssetAndRejectsAmbiguousOrMissingPayload() {
        val inline = request().apply { dataMap.putByteArray("payload", byteArrayOf(1, 2)) }
        assertArrayEquals(byteArrayOf(1, 2), ActivitySyncDataItem.read(item(inline.asPutDataRequest())) { error("must not read asset") }.payload)
        val asset = request().apply { dataMap.putAsset("payloadAsset", Asset.createFromBytes(byteArrayOf(3, 4))) }
        assertArrayEquals(byteArrayOf(3, 4), ActivitySyncDataItem.read(item(asset.asPutDataRequest())) { byteArrayOf(3, 4) }.payload)
        assertThrows(IllegalArgumentException::class.java) { ActivitySyncDataItem.read(item(request().asPutDataRequest())) { byteArrayOf() } }
        inline.dataMap.putAsset("payloadAsset", Asset.createFromBytes(byteArrayOf(3)))
        assertThrows(IllegalArgumentException::class.java) { ActivitySyncDataItem.read(item(inline.asPutDataRequest())) { byteArrayOf() } }
    }

    @Test fun boundsAssetReadsAndAckContainsOnlyMetadata() {
        assertArrayEquals(byteArrayOf(1, 2), readActivityPayload(ByteArrayInputStream(byteArrayOf(1, 2))))
        assertThrows(IllegalArgumentException::class.java) { readActivityPayload(ByteArrayInputStream(ByteArray(ActivitySyncCodec.MAX_BYTES + 1))) }
        val ack = activityAckRequest(id, 1)
        assertEquals("/activity-acks/$id", ack.uri.path)
        assertTrue(ack.assets.isEmpty())
        val map = DataMap.fromByteArray(ack.data!!)
        assertEquals(setOf("activityId", "schemaVersion"), map.keySet())
        assertEquals(id, map.getString("activityId"))
        assertEquals(1, map.getInt("schemaVersion"))
    }

    @Test fun installedManifestDispatchesActivitiesOnly() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        fun resolves(path: String) = context.packageManager.queryIntentServices(
            Intent(DataClient.ACTION_DATA_CHANGED, android.net.Uri.parse("wear://*/$path")).setPackage(context.packageName), PackageManager.MATCH_ALL
        ).any { it.serviceInfo.name == ActivitySyncListenerService::class.java.name && it.serviceInfo.exported }
        assertTrue(resolves("activities/$id"))
        assertFalse(resolves("activity-acks/$id"))
        assertFalse(resolves("other/$id"))
    }
}
