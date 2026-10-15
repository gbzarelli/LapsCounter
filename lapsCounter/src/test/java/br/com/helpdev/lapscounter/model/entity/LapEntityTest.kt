package br.com.helpdev.lapscounter.model.entity

import org.junit.Assert.assertEquals
import org.junit.Test

class LapEntityTest {
    @Test
    fun restoresCumulativeRunningTimeWithoutSubtractingPausedTime() {
        val restoredLap = LapEntity(
            chronometerTime = 8_000L,
            runningTime = 4_000L,
            pausedTime = 10_000L,
        ).toObLap()

        assertEquals(4_000L, restoredLap.getRunTime())
        assertEquals(12_000L, restoredLap.getAccumulatedTime())
        assertEquals(
            12_000L,
            LapEntity(chronometerTime = 8_000L, runningTime = 4_000L, pausedTime = 10_000L)
                .getAccumulatedRunningTime(),
        )
    }
}
