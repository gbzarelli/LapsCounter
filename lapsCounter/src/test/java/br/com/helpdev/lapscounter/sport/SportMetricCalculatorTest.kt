package br.com.helpdev.lapscounter.sport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class SportMetricCalculatorTest {
    @Test
    fun calculatesSwimmingPacePerHundredMetres() {
        assertEquals(
            SportMetricResult.Pace(totalSeconds = 120, referenceDistanceMeters = 100f),
            SportMetricCalculator.calculate(120_000, 100f, MetricDefinition.Pace(100f)),
        )
    }

    @Test
    fun calculatesRunningPacePerKilometre() {
        assertEquals(
            SportMetricResult.Pace(totalSeconds = 300, referenceDistanceMeters = 1000f),
            SportMetricCalculator.calculate(25 * 60_000L, 5000f, MetricDefinition.Pace(1000f)),
        )
    }

    @Test
    fun calculatesCyclingAverageSpeedInKilometresPerHour() {
        val result = SportMetricCalculator.calculate(40 * 60_000L, 20_000f, MetricDefinition.AverageSpeed)

        assertEquals(30.0, (result as SportMetricResult.AverageSpeed).kilometresPerHour, 0.001)
    }

    @Test
    fun returnsUnavailableForZeroTimeOrDistance() {
        assertSame(SportMetricResult.Unavailable, SportMetricCalculator.calculate(0, 100f, MetricDefinition.Pace(100f)))
        assertSame(SportMetricResult.Unavailable, SportMetricCalculator.calculate(1000, 0f, MetricDefinition.Pace(100f)))
        assertSame(SportMetricResult.Unavailable, SportMetricCalculator.calculate(0, 100f, MetricDefinition.AverageSpeed))
    }

    @Test
    fun returnsUnavailableForNegativeInputsAndInvalidPaceReference() {
        assertSame(SportMetricResult.Unavailable, SportMetricCalculator.calculate(-1000, 100f, MetricDefinition.Pace(100f)))
        assertSame(SportMetricResult.Unavailable, SportMetricCalculator.calculate(1000, -100f, MetricDefinition.AverageSpeed))
        assertSame(SportMetricResult.Unavailable, SportMetricCalculator.calculate(1000, 100f, MetricDefinition.Pace(0f)))
        assertSame(SportMetricResult.Unavailable, SportMetricCalculator.calculate(1000, 100f, MetricDefinition.Pace(-100f)))
    }

    @Test
    fun roundsPaceHalfSecondUpToTheNearestSecond() {
        assertEquals(
            SportMetricResult.Pace(totalSeconds = 1, referenceDistanceMeters = 100f),
            SportMetricCalculator.calculate(1500, 300f, MetricDefinition.Pace(100f)),
        )
    }
}
