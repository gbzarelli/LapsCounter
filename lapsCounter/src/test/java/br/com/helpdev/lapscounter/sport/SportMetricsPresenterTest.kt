package br.com.helpdev.lapscounter.sport

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class SportMetricsPresenterTest {
    @Test
    fun presentsSwimmingDistancesAndPaceInMetres() {
        val presentation = SportMetricsPresenter.present(
            runningTimeMillis = 120_000,
            splitDistanceMeters = 50f,
            travelledDistanceMeters = 100f,
            profile = SportProfile(
                sportId = SportId.SWIMMING,
                defaultLapDistanceMeters = 50f,
                distanceDisplayUnit = DistanceDisplayUnit.METRES,
                metricDefinition = MetricDefinition.Pace(100f),
            ),
            labels = SportMetricLabels("100 m", "km/h", "Unavailable"),
            locale = Locale.US,
        )

        assertEquals("50 m", presentation.splitDistance)
        assertEquals("100 m", presentation.travelledDistance)
        assertEquals("2:00 / 100 m", presentation.metric)
    }

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

    @Test
    fun usesTheSameMetricFormattingForLapAndWholeActivity() {
        val profile = SportProfile(
            sportId = SportId.SWIMMING,
            defaultLapDistanceMeters = 100f,
            distanceDisplayUnit = DistanceDisplayUnit.METRES,
            metricDefinition = MetricDefinition.Pace(100f),
        )
        val labels = SportMetricLabels("100 m", "km/h", "Unavailable")
        val lapMetric = SportMetricsPresenter.formatMetric(120_000, 100f, profile, labels, Locale.US)
        val activityMetric = SportMetricsPresenter.present(
            runningTimeMillis = 120_000,
            splitDistanceMeters = 100f,
            travelledDistanceMeters = 100f,
            profile = profile,
            labels = labels,
            locale = Locale.US,
        ).metric

        assertEquals("2:00 / 100 m", lapMetric)
        assertEquals(activityMetric, lapMetric)
    }
}
