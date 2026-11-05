package br.com.helpdev.lapscounter.wear.timer

import org.junit.Assert.*
import org.junit.Test

class WatchTimerControllerTest {
    private val clock = FakeClock(1_000)

    private fun controller(restored: WatchSessionSnapshot? = null, countLastLap: Boolean = true) =
        WatchTimerController(clock, "swimming", 50f, countLastLap, restored) { 123_456L }

    @Test fun restoresRunningSessionAfterRecreation() {
        val original = controller()
        val started = original.start()
        clock.time = 4_000
        val persisted = original.snapshot()
        clock.time = 7_000
        val restored = controller(persisted)
        assertEquals(6_000L, restored.snapshot().elapsedMillis)
        assertEquals(started.id, restored.snapshot().id)
        assertEquals(123_456L, restored.snapshot().startWallClockMillis)
        assertEquals(WatchSessionState.RUNNING, restored.snapshot().state)
    }

    @Test fun restoresPausedSessionAfterRecreation() {
        val original = controller()
        original.start()
        clock.time = 4_000
        val paused = original.pause()
        clock.time = 10_000
        val restored = controller(paused)
        assertEquals(3_000L, restored.snapshot().elapsedMillis)
        assertEquals(WatchSessionState.PAUSED, restored.snapshot().state)
        restored.resume()
        clock.time = 12_000
        assertEquals(5_000L, restored.snapshot().elapsedMillis)
    }

    @Test fun resumeDoesNotCountPausedDuration() {
        val timer = controller()
        timer.start()
        clock.time = 3_000
        timer.pause()
        clock.time = 8_000
        timer.resume()
        clock.time = 9_000
        timer.pause()
        clock.time = 12_000
        timer.resume()
        clock.time = 14_000
        assertEquals(5_000L, timer.snapshot().elapsedMillis)
        assertEquals(8_000L, timer.snapshot().accumulatedPausedMillis)
    }

    @Test fun recordsLapFromMonotonicElapsedTime() {
        val timer = controller()
        timer.start()
        clock.time = 4_000
        timer.recordLap()
        clock.time = 5_000
        timer.pause()
        clock.time = 10_000
        timer.resume()
        clock.time = 12_000
        val second = timer.recordLap()
        assertEquals(listOf(1, 2), second.laps.map { it.sequence })
        assertEquals(listOf(3_000L, 3_000L), second.laps.map { it.durationMillis })
        assertEquals(listOf(3_000L, 6_000L), second.laps.map { it.elapsedMillis })
    }

    @Test fun stopFinalizesCurrentPartialLap() {
        val timer = controller()
        timer.start()
        clock.time = 4_000
        timer.recordLap()
        clock.time = 6_000
        val stopped = timer.stop()
        clock.time = 20_000
        assertEquals(WatchSessionState.STOPPED, stopped.state)
        assertEquals(listOf(3_000L, 2_000L), stopped.laps.map { it.durationMillis })
        assertTrue(stopped.laps.last().isFinalPartialLap)
        assertEquals(100f, stopped.distanceMeters, 0f)
        assertEquals(5_000L, timer.snapshot().elapsedMillis)
    }

    @Test fun excludedFinalLapRetainsTimeButNotDistance() {
        val timer = controller(countLastLap = false)
        timer.start()
        clock.time = 3_000
        timer.recordLap()
        clock.time = 4_000
        val stopped = timer.stop()
        assertEquals(3_000L, stopped.elapsedMillis)
        assertEquals(2, stopped.laps.size)
        assertEquals(50f, stopped.distanceMeters, 0f)
    }

    @Test fun stopWhilePausedDoesNotCountPause() {
        val timer = controller()
        timer.start()
        clock.time = 3_000
        timer.pause()
        clock.time = 12_000
        assertEquals(2_000L, timer.stop().elapsedMillis)
        assertEquals(9_000L, timer.snapshot().accumulatedPausedMillis)
    }

    @Test fun invalidTransitionsCannotResetSessionOrAddLaps() {
        val timer = controller()
        assertThrows(IllegalStateException::class.java) { timer.recordLap() }
        timer.start()
        assertThrows(IllegalStateException::class.java) { timer.start() }
        timer.pause()
        assertThrows(IllegalStateException::class.java) { timer.recordLap() }
        timer.discard()
        assertEquals(WatchSessionState.DISCARDED, timer.snapshot().state)
        assertTrue(timer.snapshot().laps.isEmpty())
    }

    private class FakeClock(var time: Long) : ElapsedRealtimeClock {
        override fun nowMillis() = time
    }
}
