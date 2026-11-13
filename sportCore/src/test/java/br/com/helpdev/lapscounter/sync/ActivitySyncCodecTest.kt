package br.com.helpdev.lapscounter.sync

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class ActivitySyncCodecTest {
    private fun record(laps: Int = 2) = ActivitySyncRecord(1, "swimming", UUID.fromString("123e4567-e89b-12d3-a456-426614174000").toString(), "Swim", "notes", 123L, 25f, 50f, true, 1000L, 20L, (1..laps).map { ActivitySyncLap(it, 100L, it * 100L, it == laps) })
    // Independently specified v1: LCSY, version 1, swimming, UUID, A, no description,
    // timestamp 123, 25m/25m, true, 100ms/20ms, one final 100ms lap (big endian).
    private val goldenV1 = ("4c43535900000001000000087377696d6d696e6700000024" +
        "31323365343536372d653839622d313264332d613435362d343236363134313734303030" +
        "000000014100000000000000007b41c8000041c800000100000000000000640000000000000014" +
        "00000001000000010000000000000064000000000000006401")
        .chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    private fun goldenRecord() = ActivitySyncRecord(1, "swimming", "123e4567-e89b-12d3-a456-426614174000",
        "A", null, 123L, 25f, 25f, true, 100L, 20L, listOf(ActivitySyncLap(1, 100L, 100L, true)))
    @Test fun encodesIndependentV1GoldenBytes() { assertArrayEquals(goldenV1, ActivitySyncCodec.encode(goldenRecord())) }
    @Test fun decodesIndependentV1GoldenBytes() { assertEquals(goldenRecord(), ActivitySyncCodec.decode(goldenV1)) }

    @Test fun roundTrip() { val value = record(); assertEquals(value, ActivitySyncCodec.decode(ActivitySyncCodec.encode(value))) }
    @Test fun largeValidRecordRoundTrips() { val value = record(5000); val encoded = ActivitySyncCodec.encode(value); assertTrue(encoded.size > 90 * 1024); assertEquals(value, ActivitySyncCodec.decode(encoded)) }
    @Test fun rejectsUnknownVersion() { assertThrows(IllegalArgumentException::class.java) { ActivitySyncCodec.encode(record().copy(schemaVersion = 2)) } }
    @Test fun rejectsUnknownWireVersionAndExcessiveWireCount() {
        val version = ActivitySyncCodec.encode(record()); version[7] = 2
        assertThrows(IllegalArgumentException::class.java) { ActivitySyncCodec.decode(version) }
        val count = ActivitySyncCodec.encode(record(0)); count[count.size - 4] = 1
        assertThrows(IllegalArgumentException::class.java) { ActivitySyncCodec.decode(count) }
    }
    @Test fun rejectsTruncationAndTrailingBytes() { val bytes = ActivitySyncCodec.encode(record()); assertThrows(IllegalArgumentException::class.java) { ActivitySyncCodec.decode(bytes.dropLast(1).toByteArray()) }; assertThrows(IllegalArgumentException::class.java) { ActivitySyncCodec.decode(bytes + 0) } }
    @Test fun rejectsExcessiveLapsAndDuplicateIndices() { assertThrows(IllegalArgumentException::class.java) { ActivitySyncCodec.encode(record(10001)) }; val value = record(); assertThrows(IllegalArgumentException::class.java) { ActivitySyncCodec.encode(value.copy(laps = listOf(value.laps[0], value.laps[0]))) } }

    @Test fun rejectsInvalidIdentifiersAndNegativeOrNonFiniteMetrics() {
        val value = record()
        val invalid = listOf(
            value.copy(activityId = "invalid"),
            value.copy(activityId = "1-1-1-1-1"),
            value.copy(sportId = "unknown"),
            value.copy(startTimestampMillis = -1),
            value.copy(lapDistanceMeters = -1f),
            value.copy(lapDistanceMeters = Float.NaN),
            value.copy(travelledDistanceMeters = -1f),
            value.copy(travelledDistanceMeters = Float.POSITIVE_INFINITY),
            value.copy(runningTimeMillis = -1),
            value.copy(pausedTimeMillis = -1),
            value.copy(laps = listOf(value.laps.first().copy(index = 0))),
            value.copy(laps = listOf(value.laps.first().copy(durationMillis = -1))),
            value.copy(laps = listOf(value.laps.first().copy(elapsedMillis = -1))),
        )
        invalid.forEach { assertThrows(IllegalArgumentException::class.java) { ActivitySyncCodec.encode(it) } }
    }

    @Test fun preservesUnicodeAndOptionalDescriptionForEverySport() {
        listOf("swimming", "running", "cycling").forEach { sport ->
            val value = record().copy(sportId = sport, name = "Treino — manhã", description = null)
            assertEquals(value, ActivitySyncCodec.decode(ActivitySyncCodec.encode(value)))
        }
    }

}
