package br.com.helpdev.lapscounter.sport

object SportProfileProvider {
    fun get(): SportProfile = SportProfile(
        sportId = SportId.CYCLING,
        defaultLapDistanceMeters = 5000f,
        distanceDisplayUnit = DistanceDisplayUnit.KILOMETRES,
        metricDefinition = MetricDefinition.AverageSpeed,
    )
}
