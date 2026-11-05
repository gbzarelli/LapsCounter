package br.com.helpdev.lapscounter.sport

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class SportMetricFormatterTest {
    @Test
    fun formatsSwimmingPaceWithReferenceUnit() {
        assertEquals(
            "2:00 / 100 m",
            SportMetricFormatter.format(
                SportMetricResult.Pace(120, 100f),
                paceUnitLabel = "100 m",
                speedUnitLabel = "km/h",
                unavailableLabel = "Unavailable",
                locale = Locale.US,
            ),
        )
    }

    @Test
    fun formatsRunningPacePerKilometre() {
        assertEquals(
            "5:00 / km",
            SportMetricFormatter.format(
                SportMetricResult.Pace(300, 1000f),
                paceUnitLabel = "km",
                speedUnitLabel = "km/h",
                unavailableLabel = "Unavailable",
                locale = Locale.US,
            ),
        )
    }

    @Test
    fun formatsCyclingSpeedWithOneDecimalPlace() {
        assertEquals(
            "30.0 km/h",
            SportMetricFormatter.format(
                SportMetricResult.AverageSpeed(30.04),
                paceUnitLabel = "km",
                speedUnitLabel = "km/h",
                unavailableLabel = "Unavailable",
                locale = Locale.US,
            ),
        )
    }

    @Test
    fun formatsUnavailableUsingTheSuppliedLabel() {
        assertEquals(
            "Indisponível",
            SportMetricFormatter.format(
                SportMetricResult.Unavailable,
                paceUnitLabel = "km",
                speedUnitLabel = "km/h",
                unavailableLabel = "Indisponível",
                locale = Locale.forLanguageTag("pt-BR"),
            ),
        )
    }
}
