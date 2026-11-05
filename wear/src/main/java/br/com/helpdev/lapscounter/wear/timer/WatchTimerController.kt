package br.com.helpdev.lapscounter.wear.timer

import br.com.helpdev.lapscounter.sport.LapDistancePolicy
import br.com.helpdev.lapscounter.wear.data.WatchSyncStatus
import java.util.UUID

enum class WatchSessionState { IDLE, RUNNING, PAUSED, STOPPED, DISCARDED }

data class WatchLapSplit(
    val sequence: Int,
    val durationMillis: Long,
    val elapsedMillis: Long,
    val isFinalPartialLap: Boolean = false,
)

/** Immutable transition record. Monotonic anchors are valid across recreation within one boot. */
data class WatchSessionSnapshot(
    val id: String,
    val flavorId: String,
    val startWallClockMillis: Long?,
    val startElapsedRealtimeMillis: Long?,
    val pauseElapsedRealtimeMillis: Long?,
    val stopElapsedRealtimeMillis: Long?,
    val accumulatedPausedMillis: Long,
    val state: WatchSessionState,
    val lapDistanceMeters: Float,
    val countLastLap: Boolean,
    val laps: List<WatchLapSplit>,
    val elapsedMillis: Long,
    val syncStatus: WatchSyncStatus = WatchSyncStatus.PENDING,
) {
    fun elapsedAt(nowMillis: Long): Long {
        val start = startElapsedRealtimeMillis ?: return 0
        val end = when (state) {
            WatchSessionState.RUNNING -> nowMillis
            WatchSessionState.PAUSED -> pauseElapsedRealtimeMillis ?: start
            WatchSessionState.STOPPED -> stopElapsedRealtimeMillis ?: start
            else -> return 0
        }
        return (end - start - accumulatedPausedMillis).coerceAtLeast(0)
    }

    val distanceMeters: Float
        get() = LapDistancePolicy.countedDistanceMeters(
            totalLaps = laps.size + if (state == WatchSessionState.RUNNING || state == WatchSessionState.PAUSED) 1 else 0,
            lapDistanceMeters = lapDistanceMeters,
            isFinished = state == WatchSessionState.STOPPED,
            countLastLap = countLastLap,
        )
}

/** The caller serializes transitions and persists their returned snapshots before acknowledging them. */
class WatchTimerController(
    private val clock: ElapsedRealtimeClock = AndroidElapsedRealtimeClock,
    flavorId: String,
    lapDistanceMeters: Float,
    countLastLap: Boolean = true,
    restoredSnapshot: WatchSessionSnapshot? = null,
    private val wallClockMillis: () -> Long = System::currentTimeMillis,
) {
    private var session = restoredSnapshot ?: WatchSessionSnapshot(
        id = UUID.randomUUID().toString(),
        flavorId = flavorId,
        startWallClockMillis = null,
        startElapsedRealtimeMillis = null,
        pauseElapsedRealtimeMillis = null,
        stopElapsedRealtimeMillis = null,
        accumulatedPausedMillis = 0,
        state = WatchSessionState.IDLE,
        lapDistanceMeters = lapDistanceMeters,
        countLastLap = countLastLap,
        laps = emptyList(),
        elapsedMillis = 0,
    )

    init {
        require(flavorId.isNotBlank())
        require(lapDistanceMeters.isFinite() && lapDistanceMeters > 0)
        require(restoredSnapshot == null || restoredSnapshot.flavorId == flavorId)
    }

    fun start(): WatchSessionSnapshot {
        check(session.state == WatchSessionState.IDLE)
        session = session.copy(
            startWallClockMillis = wallClockMillis(),
            startElapsedRealtimeMillis = clock.nowMillis(),
            state = WatchSessionState.RUNNING,
        )
        return snapshot()
    }

    fun pause(): WatchSessionSnapshot {
        check(session.state == WatchSessionState.RUNNING)
        session = session.copy(state = WatchSessionState.PAUSED, pauseElapsedRealtimeMillis = clock.nowMillis())
        return snapshot()
    }

    fun resume(): WatchSessionSnapshot {
        check(session.state == WatchSessionState.PAUSED)
        val now = clock.nowMillis()
        session = session.copy(
            state = WatchSessionState.RUNNING,
            accumulatedPausedMillis = session.accumulatedPausedMillis + now - requireNotNull(session.pauseElapsedRealtimeMillis),
            pauseElapsedRealtimeMillis = null,
        )
        return snapshot()
    }

    fun recordLap(): WatchSessionSnapshot {
        check(session.state == WatchSessionState.RUNNING)
        val elapsed = session.elapsedAt(clock.nowMillis())
        session = session.copy(laps = session.laps + split(elapsed, isFinal = false))
        return snapshot()
    }

    fun stop(): WatchSessionSnapshot {
        check(session.state == WatchSessionState.RUNNING || session.state == WatchSessionState.PAUSED)
        val now = clock.nowMillis()
        val elapsed = session.elapsedAt(now)
        val finalPause = session.pauseElapsedRealtimeMillis?.let { now - it } ?: 0
        session = session.copy(
            state = WatchSessionState.STOPPED,
            stopElapsedRealtimeMillis = now,
            pauseElapsedRealtimeMillis = null,
            accumulatedPausedMillis = session.accumulatedPausedMillis + finalPause,
            laps = session.laps + split(elapsed, isFinal = true),
        )
        return snapshot()
    }

    fun discard(): WatchSessionSnapshot {
        session = session.copy(state = WatchSessionState.DISCARDED, laps = emptyList(), elapsedMillis = 0)
        return snapshot()
    }

    fun snapshot(): WatchSessionSnapshot = session.copy(elapsedMillis = session.elapsedAt(clock.nowMillis()))

    private fun split(elapsed: Long, isFinal: Boolean) = WatchLapSplit(
        sequence = session.laps.size + 1,
        durationMillis = elapsed - (session.laps.lastOrNull()?.elapsedMillis ?: 0),
        elapsedMillis = elapsed,
        isFinalPartialLap = isFinal,
    )
}
