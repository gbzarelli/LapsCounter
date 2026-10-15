package br.com.helpdev.lapscounter.sport

object SportProfileProvider {
    fun get(): SportProfile = SportProfile(
        sportId = SportId.RUNNING,
        defaultLapDistanceMeters = 1000f,
        distanceDisplayUnit = DistanceDisplayUnit.KILOMETRES,
        metricDefinition = MetricDefinition.Pace(referenceDistanceMeters = 1000f),
    )
}
