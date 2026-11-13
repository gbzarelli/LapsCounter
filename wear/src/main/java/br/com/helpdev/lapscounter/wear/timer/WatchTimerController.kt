package br.com.helpdev.lapscounter.wear.timer

import br.com.helpdev.lapscounter.sport.LapDistancePolicy
import br.com.helpdev.lapscounter.wear.data.WatchSyncStatus
import java.util.UUID

enum class WatchSessionState { IDLE, RUNNING, PAUSED, STOPPED, RECOVERY_REQUIRED, DISCARDED }

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
    val bootIdentity: String? = null,
    val checkpointRealtimeMillis: Long? = null,
) {
    fun elapsedAt(nowMillis: Long): Long {
        if (state == WatchSessionState.RECOVERY_REQUIRED || state == WatchSessionState.STOPPED) return elapsedMillis
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
            totalLaps = laps.size + if (state in setOf(WatchSessionState.RUNNING, WatchSessionState.PAUSED, WatchSessionState.RECOVERY_REQUIRED)) 1 else 0,
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
        detectClockReset()
    }

    fun start(): WatchSessionSnapshot {
        detectClockReset()
        check(session.state == WatchSessionState.IDLE)
        session = session.copy(
            startWallClockMillis = wallClockMillis(),
            startElapsedRealtimeMillis = clock.nowMillis(),
            state = WatchSessionState.RUNNING,
            bootIdentity = clock.bootIdentity(),
        )
        return snapshot()
    }

    fun pause(): WatchSessionSnapshot {
        detectClockReset()
        check(session.state == WatchSessionState.RUNNING)
        session = session.copy(state = WatchSessionState.PAUSED, pauseElapsedRealtimeMillis = clock.nowMillis())
        return snapshot()
    }

    fun resume(): WatchSessionSnapshot {
        detectClockReset()
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
        detectClockReset()
        check(session.state == WatchSessionState.RUNNING)
        val elapsed = session.elapsedAt(clock.nowMillis())
        session = session.copy(laps = session.laps + split(elapsed, isFinal = false))
        return snapshot()
    }

    fun stop(): WatchSessionSnapshot {
        detectClockReset()
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
            elapsedMillis = elapsed,
        )
        return snapshot()
    }

    fun discard(): WatchSessionSnapshot {
        session = session.copy(state = WatchSessionState.DISCARDED, laps = emptyList(), elapsedMillis = 0)
        return snapshot()
    }

    /** Explicitly end at the last durable checkpoint; powered-off time is unknown. */
    fun finishAtCheckpoint(): WatchSessionSnapshot {
        check(session.state == WatchSessionState.RECOVERY_REQUIRED)
        session = session.copy(
            state = WatchSessionState.STOPPED,
            stopElapsedRealtimeMillis = session.checkpointRealtimeMillis,
            accumulatedPausedMillis = session.accumulatedPausedMillis + (session.pauseElapsedRealtimeMillis?.let {
                ((session.checkpointRealtimeMillis ?: it) - it).coerceAtLeast(0)
            } ?: 0),
            pauseElapsedRealtimeMillis = null,
            laps = session.laps + split(session.elapsedMillis, isFinal = true),
        )
        return snapshot()
    }

    fun snapshot(): WatchSessionSnapshot {
        detectClockReset()
        if (session.state == WatchSessionState.RUNNING || session.state == WatchSessionState.PAUSED) {
            val now = clock.nowMillis()
            session = session.copy(elapsedMillis = session.elapsedAt(now), checkpointRealtimeMillis = now)
        }
        return session
    }

    private fun detectClockReset() {
        if (session.state != WatchSessionState.RUNNING && session.state != WatchSessionState.PAUSED) return
        val now = clock.nowMillis()
        val previous = session.checkpointRealtimeMillis ?: listOfNotNull(
            session.startElapsedRealtimeMillis, session.pauseElapsedRealtimeMillis,
        ).maxOrNull() ?: now
        val boot = clock.bootIdentity()
        // Legacy active records with no boot identity are ambiguous on production clocks.
        if (now < previous || boot == null || session.bootIdentity == null || boot != session.bootIdentity) {
            session = session.copy(state = WatchSessionState.RECOVERY_REQUIRED)
        }
    }

    private fun split(elapsed: Long, isFinal: Boolean) = WatchLapSplit(
        sequence = session.laps.size + 1,
        durationMillis = elapsed - (session.laps.lastOrNull()?.elapsedMillis ?: 0),
        elapsedMillis = elapsed,
        isFinalPartialLap = isFinal,
    )
}
