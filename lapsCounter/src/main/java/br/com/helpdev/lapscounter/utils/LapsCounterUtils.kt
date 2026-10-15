package br.com.helpdev.lapscounter.utils

import br.com.helpdev.chronometerlib.Chronometer
import br.com.helpdev.lapscounter.model.entity.ActivityEntity
import br.com.helpdev.lapscounter.sport.LapDistancePolicy
import br.com.helpdev.lapscounter.sport.SportMetricCalculator
import br.com.helpdev.lapscounter.sport.SportMetricResult
import br.com.helpdev.lapscounter.sport.SportProfile
import br.com.helpdev.lapscounter.sport.SportProfileProvider

object LapsCounterUtils {
    fun getMetric(
        chronometer: Chronometer,
        lapDistanceMeters: Float,
        countLastLap: Boolean,
        profile: SportProfile = SportProfileProvider.get(),
    ): SportMetricResult = SportMetricCalculator.calculate(
        runningTimeMillis = getRunningTime(chronometer, countLastLap),
        distanceMeters = getDistanceTravelled(chronometer, lapDistanceMeters, countLastLap),
        definition = profile.metricDefinition,
    )

    fun getMetric(
        activityEntity: ActivityEntity,
        profile: SportProfile = SportProfileProvider.get(),
    ): SportMetricResult = SportMetricCalculator.calculate(
        runningTimeMillis = getRunningTime(activityEntity),
        distanceMeters = activityEntity.travelledDistance,
        definition = profile.metricDefinition,
    )

    fun getRunningTime(chronometer: Chronometer, countLastLap: Boolean): Long {
        val countedLapCount = countedLapCount(chronometer, countLastLap)
        return chronometer.getLaps()
            .take(countedLapCount)
            .sumOf { it.getRunTime().coerceAtLeast(0L) }
    }

    fun getRunningTime(activityEntity: ActivityEntity): Long {
        val laps = activityEntity.chronometer?.laps ?: return 0L
        val countedLapCount = LapDistancePolicy.countedLapCount(
            totalLaps = laps.size,
            isFinished = true,
            countLastLap = activityEntity.countLastLap,
        )
        return laps.take(countedLapCount).sumOf { it.runningTime.coerceAtLeast(0L) }
    }

    fun getDistanceTravelled(
        chronometer: Chronometer,
        lapDistanceMeters: Float,
        countLastLap: Boolean,
    ): Float = LapDistancePolicy.countedDistanceMeters(
        totalLaps = chronometer.getLaps().size,
        lapDistanceMeters = lapDistanceMeters,
        isFinished = chronometer.isFinished() && chronometer.getChronometerTime() > 0,
        countLastLap = countLastLap,
    )

    private fun countedLapCount(chronometer: Chronometer, countLastLap: Boolean) =
        LapDistancePolicy.countedLapCount(
            totalLaps = chronometer.getLaps().size,
            isFinished = chronometer.isFinished() && chronometer.getChronometerTime() > 0,
            countLastLap = countLastLap,
        )

    private fun Chronometer.isFinished() = endTime > 0
}
