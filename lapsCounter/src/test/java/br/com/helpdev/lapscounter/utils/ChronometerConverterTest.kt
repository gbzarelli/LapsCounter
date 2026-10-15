package br.com.helpdev.lapscounter.utils

import br.com.helpdev.chronometerlib.ChronometerManager
import br.com.helpdev.lapscounter.model.entity.toObLaps
import br.com.helpdev.lapscounter.model.entity.getAccumulatedRunningTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ChronometerConverterTest {
    @Test
    fun preservesRunningAndPausedLapTimesAcrossSaveAndRestore() {
        var now = 1_000L
        val manager = ChronometerManager { now }

        manager.start()
        now = 5_000L
        manager.lap()
        now = 6_000L
        manager.pause()
        now = 11_000L
        manager.start()
        now = 14_000L
        manager.lap()
        now = 16_000L
        manager.stop()

        val savedActivity = manager.chronometer.toActivityEntity("test", "", 50f)
        val savedLaps = savedActivity.chronometer!!.laps
        val restoredLaps = savedActivity.chronometer!!.toObLaps()

        assertEquals(listOf(4_000L, 4_000L, 2_000L), savedLaps.map { it.runningTime })
        assertEquals(listOf(0L, 5_000L, 0L), savedLaps.map { it.pausedTime })
        assertEquals(listOf(4_000L, 8_000L, 10_000L), restoredLaps.map { it.getAccumulatedTime() })
        assertEquals(
            listOf(4_000L, 8_000L, 10_000L),
            savedLaps.map { it.getAccumulatedRunningTime() },
        )
    }
}
