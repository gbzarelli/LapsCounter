package br.com.helpdev.lapscounter.wear

import br.com.helpdev.lapscounter.sport.SportId
import br.com.helpdev.lapscounter.sport.SportProfileProvider
import org.junit.Assert.assertEquals
import org.junit.Test

class WearBuildConfigContractTest {
    @Test
    fun applicationIdMatchesSportFlavor() {
        val (expectedApplicationId, expectedSport) = when (BuildConfig.FLAVOR) {
            "swimming" -> "br.com.helpdev.lapscounter.swimming.debug" to SportId.SWIMMING
            "running" -> "br.com.helpdev.lapscounter.running.debug" to SportId.RUNNING
            "cycling" -> "br.com.helpdev.lapscounter.cycling.debug" to SportId.CYCLING
            else -> throw AssertionError("Unsupported flavor: ${BuildConfig.FLAVOR}")
        }

        assertEquals(expectedApplicationId, BuildConfig.APPLICATION_ID)
        assertEquals(expectedSport, SportProfileProvider.get().sportId)
    }

    @Test
    fun versionCodeUsesWearFormFactorOffset() {
        assertEquals(1_000_012, BuildConfig.VERSION_CODE)
    }
}
