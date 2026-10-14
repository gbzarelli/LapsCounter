package br.com.helpdev.lapscounter.sport

import kotlin.math.roundToLong

object SportMetricCalculator {
    fun calculate(
        runningTimeMillis: Long,
        distanceMeters: Float,
        definition: MetricDefinition,
    ): SportMetricResult {
        if (runningTimeMillis <= 0 || !distanceMeters.isFinite() || distanceMeters <= 0f) {
            return SportMetricResult.Unavailable
        }

        return when (definition) {
            is MetricDefinition.Pace -> {
                val referenceDistanceMeters = definition.referenceDistanceMeters
                if (!referenceDistanceMeters.isFinite() || referenceDistanceMeters <= 0f) {
                    SportMetricResult.Unavailable
                } else {
                    val totalSeconds = (
                        runningTimeMillis.toDouble() * referenceDistanceMeters.toDouble() /
                            distanceMeters.toDouble() / 1000.0
                        ).roundToLong()
                    SportMetricResult.Pace(totalSeconds, referenceDistanceMeters)
                }
            }

            MetricDefinition.AverageSpeed -> {
                val kilometresPerHour = distanceMeters.toDouble() * 3600.0 / runningTimeMillis.toDouble()
                if (kilometresPerHour.isFinite()) {
                    SportMetricResult.AverageSpeed(kilometresPerHour)
                } else {
                    SportMetricResult.Unavailable
                }
            }
        }
    }
}

sealed class SportMetricResult {
    data class Pace(
        val totalSeconds: Long,
        val referenceDistanceMeters: Float,
    ) : SportMetricResult()

    data class AverageSpeed(val kilometresPerHour: Double) : SportMetricResult()

    object Unavailable : SportMetricResult()
}
