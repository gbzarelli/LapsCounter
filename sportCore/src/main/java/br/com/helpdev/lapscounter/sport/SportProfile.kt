package br.com.helpdev.lapscounter.sport

data class SportProfile(
    val sportId: SportId,
    val defaultLapDistanceMeters: Float,
    val distanceDisplayUnit: DistanceDisplayUnit,
    val metricDefinition: MetricDefinition,
)

enum class SportId {
    SWIMMING,
    RUNNING,
    CYCLING,
}

enum class DistanceDisplayUnit(val metersPerUnit: Float) {
    METRES(1f),
    KILOMETRES(1000f),
}

sealed class MetricDefinition {
    data class Pace(val referenceDistanceMeters: Float) : MetricDefinition()

    object AverageSpeed : MetricDefinition()
}
