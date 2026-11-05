package br.com.helpdev.lapscounter.wear.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.helpdev.lapscounter.sync.*
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataItemAsset
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataLayerActivitySyncTransportTest {
    @Test fun choosesInlineThroughLimitAndAssetAboveIt() = runBlocking {
        val id = "123e4567-e89b-12d3-a456-426614174000"
        val base = ActivitySyncRecord(1, "swimming", id, "Swim", null, 1, 25f, 25f, true, 100, 0, emptyList())
        var request: PutDataRequest? = null
        val transport = DataLayerActivitySyncTransport(DataClientAdapter { request = it })
        transport.send(base)
        val small = DataMap.fromByteArray(requireNotNull(request!!.data))
        assertEquals(id, small.getString("activityId"))
        assertEquals(1, small.getInt("schemaVersion"))
        assertEquals(base, ActivitySyncCodec.decode(requireNotNull(small.getByteArray("payload"))))
        assertTrue(request!!.assets.isEmpty())
        val overhead = ActivitySyncCodec.encode(base.copy(description = "")).size
        val exact = base.copy(description = "a".repeat(65536), laps = List((90 * 1024 - overhead - 65536) / 21) { ActivitySyncLap(it + 1, 0, 0, false) })
        // Fill remaining bytes with the name (whose original length is four bytes).
        val sized = exact.copy(name = "S".repeat(4 + 90 * 1024 - ActivitySyncCodec.encode(exact).size))
        assertEquals(90 * 1024, ActivitySyncCodec.encode(sized).size)
        transport.send(sized)
        val inline = requireNotNull(request); val inlineMap = DataMap.fromByteArray(requireNotNull(inline.data))
        assertEquals("/activities/$id", inline.uri.path); assertEquals(1, inlineMap.getInt("schemaVersion")); assertNotNull(inlineMap.getByteArray("payload")); assertTrue(inline.assets.isEmpty())
        transport.send(sized.copy(name = sized.name + "x"))
        val asset = requireNotNull(request)
        assertEquals("/activities/$id", asset.uri.path)
        val assetMap = DataMapItem.fromDataItem(object : DataItem {
            override fun getUri() = asset.uri
            override fun getData() = asset.data
            override fun setData(data: ByteArray?): DataItem = error("Read-only test item")
            override fun getAssets() = asset.assets.mapValues { (key, _) ->
                object : DataItemAsset {
                    override fun getId() = "test-digest"
                    override fun getDataItemKey() = key
                    override fun freeze(): DataItemAsset = this
                    override fun isDataValid() = true
                }
            }
            override fun freeze(): DataItem = this
            override fun isDataValid() = true
        }).dataMap
        assertEquals(id, assetMap.getString("activityId"))
        assertEquals(1, assetMap.getInt("schemaVersion"))
        assertNull(assetMap.getByteArray("payload"))
        assertNotNull(assetMap.getAsset("payloadAsset"))
        assertEquals(Asset.createFromBytes(ActivitySyncCodec.encode(sized.copy(name = sized.name + "x"))), asset.getAsset("0"))
    }
}
