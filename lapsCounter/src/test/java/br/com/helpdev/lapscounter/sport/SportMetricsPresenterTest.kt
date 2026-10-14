package br.com.helpdev.lapscounter.sport

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class SportMetricsPresenterTest {
    @Test
    fun presentsRunningDistancesAndPaceInMetricUnits() {
        val presentation = SportMetricsPresenter.present(
            runningTimeMillis = 25 * 60_000L,
            splitDistanceMeters = 1000f,
            travelledDistanceMeters = 5000f,
            profile = SportProfile(
                sportId = SportId.RUNNING,
                defaultLapDistanceMeters = 1000f,
                distanceDisplayUnit = DistanceDisplayUnit.KILOMETRES,
                metricDefinition = MetricDefinition.Pace(1000f),
            ),
            labels = SportMetricLabels("km", "km/h", "Unavailable"),
            locale = Locale.US,
        )

        assertEquals("1 km", presentation.splitDistance)
        assertEquals("5 km", presentation.travelledDistance)
        assertEquals("5:00 / km", presentation.metric)
    }

    @Test
    fun presentsCyclingAverageSpeed() {
        val presentation = SportMetricsPresenter.present(
            runningTimeMillis = 40 * 60_000L,
            splitDistanceMeters = 5000f,
            travelledDistanceMeters = 20_000f,
            profile = SportProfile(
                sportId = SportId.CYCLING,
                defaultLapDistanceMeters = 5000f,
                distanceDisplayUnit = DistanceDisplayUnit.KILOMETRES,
                metricDefinition = MetricDefinition.AverageSpeed,
            ),
            labels = SportMetricLabels("km", "km/h", "Unavailable"),
            locale = Locale.US,
        )

        assertEquals("5 km", presentation.splitDistance)
        assertEquals("20 km", presentation.travelledDistance)
        assertEquals("30.0 km/h", presentation.metric)
    }
}
