package br.com.helpdev.lapscounter.sport

import br.com.helpdev.lapscounter.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SportProfileProviderTest {
    @Test
    fun flavorProvidesItsSportDefaultsAndMetric() {
        val profile = SportProfileProvider.get()

        when (BuildConfig.FLAVOR) {
            "swimming" -> {
                assertEquals(SportId.SWIMMING, profile.sportId)
                assertEquals(50f, profile.defaultLapDistanceMeters)
                assertEquals(DistanceDisplayUnit.METRES, profile.distanceDisplayUnit)
                assertTrue(profile.metricDefinition is MetricDefinition.Pace)
                assertEquals(100f, (profile.metricDefinition as MetricDefinition.Pace).referenceDistanceMeters)
            }

            "running" -> {
                assertEquals(SportId.RUNNING, profile.sportId)
                assertEquals(1000f, profile.defaultLapDistanceMeters)
                assertEquals(DistanceDisplayUnit.KILOMETRES, profile.distanceDisplayUnit)
                assertTrue(profile.metricDefinition is MetricDefinition.Pace)
                assertEquals(1000f, (profile.metricDefinition as MetricDefinition.Pace).referenceDistanceMeters)
            }

            "cycling" -> {
                assertEquals(SportId.CYCLING, profile.sportId)
                assertEquals(5000f, profile.defaultLapDistanceMeters)
                assertEquals(DistanceDisplayUnit.KILOMETRES, profile.distanceDisplayUnit)
                assertEquals(MetricDefinition.AverageSpeed, profile.metricDefinition)
            }

            else -> throw AssertionError("Unsupported flavor: ${BuildConfig.FLAVOR}")
        }
    }
}
