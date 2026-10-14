package br.com.helpdev.lapscounter.sport

import org.junit.Assert.assertEquals
import org.junit.Test

class LapDistancePolicyTest {
    @Test
    fun excludesTheCurrentLapWhileTimerIsRunning() {
        assertEquals(3, LapDistancePolicy.countedLapCount(totalLaps = 4, isFinished = false, countLastLap = true))
        assertEquals(3, LapDistancePolicy.countedLapCount(totalLaps = 4, isFinished = false, countLastLap = false))
    }

    @Test
    fun includesFinalLapWhenFinishedAndConfiguredToCountIt() {
        assertEquals(4, LapDistancePolicy.countedLapCount(totalLaps = 4, isFinished = true, countLastLap = true))
    }

    @Test
    fun excludesFinalLapWhenFinishedAndConfiguredNotToCountIt() {
        assertEquals(3, LapDistancePolicy.countedLapCount(totalLaps = 4, isFinished = true, countLastLap = false))
    }

    @Test
    fun clampsZeroLapCountsAndDistances() {
        assertEquals(0, LapDistancePolicy.countedLapCount(totalLaps = 0, isFinished = true, countLastLap = true))
        assertEquals(0, LapDistancePolicy.countedLapCount(totalLaps = 0, isFinished = false, countLastLap = true))
        assertEquals(0f, LapDistancePolicy.countedDistanceMeters(0, 50f, true, true))
    }

    @Test
    fun calculatesDistanceFromCountedLaps() {
        assertEquals(150f, LapDistancePolicy.countedDistanceMeters(4, 50f, false, true))
        assertEquals(200f, LapDistancePolicy.countedDistanceMeters(4, 50f, true, true))
        assertEquals(150f, LapDistancePolicy.countedDistanceMeters(4, 50f, true, false))
    }
}
