package br.com.helpdev.lapscounter.utils

import br.com.helpdev.chronometerlib.Chronometer
import br.com.helpdev.lapscounter.BuildConfig
import br.com.helpdev.lapscounter.sport.SportId
import br.com.helpdev.lapscounter.sport.SportMetricResult
import br.com.helpdev.lapscounter.sport.SportProfileProvider
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class LapsCounterUtilsTest {
    @Test
    fun stoppedActivityExcludesTheFinalLapFromDistanceAndMetric() {
        val profile = SportProfileProvider.get()
        val expectedSportId = when (BuildConfig.FLAVOR) {
            "swimming" -> SportId.SWIMMING
            "running" -> SportId.RUNNING
            "cycling" -> SportId.CYCLING
            else -> throw AssertionError("Unsupported flavor: ${BuildConfig.FLAVOR}")
        }
        assertEquals(expectedSportId, profile.sportId)
        val lapRunningTimeMillis = when (profile.sportId) {
            SportId.SWIMMING -> 60_000L
            SportId.RUNNING -> 300_000L
            SportId.CYCLING -> 600_000L
        }
        val chronometer = finishedChronometer(
            firstLapRunningTimeMillis = lapRunningTimeMillis,
            finalLapRunningTimeMillis = lapRunningTimeMillis * 4,
        )

        assertEquals(
            profile.defaultLapDistanceMeters,
            LapsCounterUtils.getDistanceTravelled(
                chronometer,
                profile.defaultLapDistanceMeters,
                countLastLap = false,
            ),
        )
        assertEquals(
            expectedMetric(profile.sportId),
            LapsCounterUtils.getMetric(
                chronometer,
                profile.defaultLapDistanceMeters,
                countLastLap = false,
            ),
        )
    }

    @Test
    fun emptyActivityHasUnavailableMetricForEveryFlavor() {
        assertSame(
            SportMetricResult.Unavailable,
            LapsCounterUtils.getMetric(Chronometer(), 50f, countLastLap = false),
        )
    }

    private fun expectedMetric(sportId: SportId): SportMetricResult = when (sportId) {
        SportId.SWIMMING -> SportMetricResult.Pace(totalSeconds = 120, referenceDistanceMeters = 100f)
        SportId.RUNNING -> SportMetricResult.Pace(totalSeconds = 300, referenceDistanceMeters = 1000f)
        SportId.CYCLING -> SportMetricResult.AverageSpeed(kilometresPerHour = 30.0)
    }

    private fun finishedChronometer(firstLapRunningTimeMillis: Long, finalLapRunningTimeMillis: Long): Chronometer {
        fun lap(runningTimeMillis: Long) = Chronometer.Companion.Builder(
            Date(),
            accumulatedStartTime = 0,
            startTime = 0,
            endTime = runningTimeMillis,
            pausedTime = 0,
        ).build()

        return Chronometer.Companion.Builder(
            Date(),
            accumulatedStartTime = 0,
            startTime = 0,
            endTime = 1,
            pausedTime = 0,
            laps = listOf(lap(firstLapRunningTimeMillis), lap(finalLapRunningTimeMillis)),
        ).build()
    }
}
