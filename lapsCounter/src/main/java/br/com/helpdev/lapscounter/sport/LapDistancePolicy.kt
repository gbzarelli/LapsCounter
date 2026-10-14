package br.com.helpdev.lapscounter.sport

object LapDistancePolicy {
    fun countedLapCount(totalLaps: Int, isFinished: Boolean, countLastLap: Boolean): Int {
        if (totalLaps <= 0) return 0
        return if (isFinished && countLastLap) totalLaps else totalLaps - 1
    }

    fun countedDistanceMeters(
        totalLaps: Int,
        lapDistanceMeters: Float,
        isFinished: Boolean,
        countLastLap: Boolean,
    ): Float = countedLapCount(totalLaps, isFinished, countLastLap) * lapDistanceMeters
}
