package br.com.helpdev.lapscounter.sport

import org.junit.Assert.assertEquals
import org.junit.Test

class LapDistancePreferenceTest {
    @Test
    fun preservesExistingSwimmingValuesInMetres() {
        assertEquals(50f, LapDistancePreferenceAdapter.toDisplayValue(50f, DistanceDisplayUnit.METRES))
        assertEquals(50f, LapDistancePreferenceAdapter.toStoredMeters(50f, DistanceDisplayUnit.METRES))
        assertEquals(375f, LapDistancePreferenceAdapter.toStoredMeters(375f, DistanceDisplayUnit.METRES))
    }

    @Test
    fun convertsRunningKilometresToAndFromStoredMetres() {
        val displayValue = LapDistancePreferenceAdapter.toDisplayValue(1000f, DistanceDisplayUnit.KILOMETRES)

        assertEquals(1f, displayValue)
        assertEquals(1000f, LapDistancePreferenceAdapter.toStoredMeters(displayValue, DistanceDisplayUnit.KILOMETRES))
    }

    @Test
    fun convertsCyclingDefaultAndFractionalKilometres() {
        assertEquals(5f, LapDistancePreferenceAdapter.toDisplayValue(5000f, DistanceDisplayUnit.KILOMETRES))
        assertEquals(5000f, LapDistancePreferenceAdapter.toStoredMeters(5f, DistanceDisplayUnit.KILOMETRES))
        assertEquals(1250f, LapDistancePreferenceAdapter.toStoredMeters(1.25f, DistanceDisplayUnit.KILOMETRES))
        assertEquals(1.25f, LapDistancePreferenceAdapter.toDisplayValue(1250f, DistanceDisplayUnit.KILOMETRES))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonPositiveDistanceBeforePersistence() {
        LapDistancePreferenceAdapter.toStoredMeters(0f, DistanceDisplayUnit.KILOMETRES)
    }
}
