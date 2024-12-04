package br.com.helpdev.lapscounter.utils

import br.com.helpdev.chronometerlib.Chronometer
import br.com.helpdev.lapscounter.model.entity.ActivityEntity

object LapsCounterUtils {
    fun getPace(activityEntity: ActivityEntity) =
        getPace(getRunningTimeToPace(activityEntity), activityEntity.travelledDistance)

    private fun getRunningTimeToPace(activityEntity: ActivityEntity): Long {
        val removedTimeToPace = if (activityEntity.countLastLap) {
            0
        } else
            activityEntity.chronometer!!.laps.last()?.runningTime ?: 0
        return (activityEntity.chronometer!!.runningTime - removedTimeToPace)
    }

    fun getPace(
        chronometer: Chronometer,
        lapDistance: Float,
        countLastLap: Boolean
    ): Pair<Long, Long> {
        val distanceTravelled = getDistanceTravelled(chronometer, lapDistance, countLastLap)

        if (distanceTravelled <= 0) return Pair(0, 0)

        var runningTime = 0L

        val cut = countLastLap(chronometer, countLastLap)

        for (x in 0 until (chronometer.getLaps().size - cut)) {
            val obLap = chronometer.getLaps()[x]
            runningTime += obLap.getRunTime() // ?? can be chronometerTime ?
        }

        return getPace(runningTime, distanceTravelled)
    }

    fun getPace(runningTime: Long, distanceTravelled: Float): Pair<Long, Long> {
        val pace = ((runningTime * 100) / distanceTravelled).toLong()
        return Pair(pace / 60_000L, (pace % 60_000L) / 1000L)
    }

    fun getDistanceTravelled(
        chronometer: Chronometer,
        lapDistance: Float,
        countLastLap: Boolean
    ): Float {
        if (chronometer.getLaps().isEmpty()) return 0F
        val cut = countLastLap(chronometer, countLastLap)
        return (chronometer.getLaps().size - cut) * lapDistance
    }

    private fun countLastLap(chronometer: Chronometer, countLastLap: Boolean) =
        if (countLastLap && chronometer.getChronometerTime() > 0 && chronometer.isFinished())
            0
        else
            1

    private fun Chronometer.isFinished() = endTime > 0
}
