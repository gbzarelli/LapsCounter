package br.com.helpdev.lapscounter.sync

import br.com.helpdev.lapscounter.model.entity.getAccumulatedRunningTime
import org.junit.Assert.*
import org.junit.Test

class ActivitySyncImporterTest {
    private fun record() = ActivitySyncRecord(
        1, "swimming", "123e4567-e89b-12d3-a456-426614174000", "Morning swim", "Pool",
        1_700_000_000_000L, 50f, 100f, false, 9_000, 2_000,
        listOf(ActivitySyncLap(1, 4_000, 4_000, false), ActivitySyncLap(2, 3_000, 7_000, false), ActivitySyncLap(3, 2_000, 9_000, true))
    )

    @Test fun mapsUuidMetadataTotalsAndOrderedLapRunningTimes() {
        val entity = ActivitySyncImporter.toActivityEntity(record(), "swimming")
        assertEquals("123e4567-e89b-12d3-a456-426614174000", entity.id)
        assertEquals("Morning swim", entity.name)
        assertEquals("Pool", entity.description)
        assertEquals(1_700_000_000_000L, entity.dateStarted!!.time)
        assertEquals(50f, entity.lapDistance)
        assertEquals(100f, entity.travelledDistance)
        assertFalse(entity.countLastLap)
        val timer = entity.chronometer!!
        assertEquals(9_000L, timer.runningTime)
        assertEquals(2_000L, timer.pausedTime)
        assertEquals(listOf(0L, 4_000L, 7_000L), timer.laps.map { it.chronometerTime })
        assertEquals(listOf(4_000L, 3_000L, 2_000L), timer.laps.map { it.runningTime })
        assertEquals(listOf(4_000L, 7_000L, 9_000L), timer.laps.map { it.getAccumulatedRunningTime() })
        assertEquals(listOf(0L, 0L, 0L), timer.laps.map { it.pausedTime })
    }

    @Test fun preservesNullableDescriptionAndIncludedFinalLap() {
        val entity = ActivitySyncImporter.toActivityEntity(record().copy(description = null, countLastLap = true, travelledDistanceMeters = 150f), "swimming")
        assertNull(entity.description)
        assertTrue(entity.countLastLap)
        assertEquals(150f, entity.travelledDistance)
        assertEquals(3, entity.chronometer!!.laps.size)
    }

    @Test fun rejectsWrongFlavorAndUnknownVersion() {
        assertThrows(IllegalArgumentException::class.java) { ActivitySyncImporter.toActivityEntity(record(), "running") }
        assertThrows(IllegalArgumentException::class.java) { ActivitySyncImporter.toActivityEntity(record().copy(schemaVersion = 2), "swimming") }
    }

    @Test fun rejectsInvalidPayloadValuesWithoutReturningPartialEntity() {
        val invalid = listOf(
            record().copy(runningTimeMillis = 8_999),
            record().copy(laps = listOf(ActivitySyncLap(1, 4_000, 4_000, true), ActivitySyncLap(2, 5_000, 9_000, true))),
            record().copy(laps = listOf(ActivitySyncLap(1, 4_000, 4_000, false), ActivitySyncLap(2, 4_000, 9_000, true))),
            record().copy(activityId = "invalid"), record().copy(name = " "),
            record().copy(startTimestampMillis = -1), record().copy(lapDistanceMeters = Float.NaN),
            record().copy(travelledDistanceMeters = -1f), record().copy(runningTimeMillis = -1),
            record().copy(pausedTimeMillis = -1), record().copy(laps = listOf(ActivitySyncLap(1, -1, 0, false))),
            record().copy(laps = listOf(ActivitySyncLap(1, 5, 4, false))),
            record().copy(laps = listOf(ActivitySyncLap(1, 4, 4, false), ActivitySyncLap(1, 5, 9, true)))
        )
        invalid.forEach { bad -> assertThrows(IllegalArgumentException::class.java) { ActivitySyncImporter.toActivityEntity(bad, "swimming") } }
    }
}
