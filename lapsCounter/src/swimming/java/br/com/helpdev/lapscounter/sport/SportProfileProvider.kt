package br.com.helpdev.lapscounter.sport

object SportProfileProvider {
    fun get(): SportProfile = SportProfile(
        sportId = SportId.SWIMMING,
        defaultLapDistanceMeters = 50f,
        distanceDisplayUnit = DistanceDisplayUnit.METRES,
        metricDefinition = MetricDefinition.Pace(referenceDistanceMeters = 100f),
    )
}
