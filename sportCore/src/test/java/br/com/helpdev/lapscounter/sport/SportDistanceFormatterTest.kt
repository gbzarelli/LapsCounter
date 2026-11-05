package br.com.helpdev.lapscounter.sport

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class SportDistanceFormatterTest {
    @Test
    fun formatsSwimmingDistanceInMetres() {
        assertEquals("50 m", SportDistanceFormatter.format(50f, DistanceDisplayUnit.METRES, Locale.US))
    }

    @Test
    fun formatsWholeAndFractionalKilometres() {
        assertEquals("1 km", SportDistanceFormatter.format(1000f, DistanceDisplayUnit.KILOMETRES, Locale.US))
        assertEquals("1.25 km", SportDistanceFormatter.format(1250f, DistanceDisplayUnit.KILOMETRES, Locale.US))
    }

    @Test
    fun localizesFractionalDistanceDecimalSeparator() {
        assertEquals(
            "1,25 km",
            SportDistanceFormatter.format(1250f, DistanceDisplayUnit.KILOMETRES, Locale.forLanguageTag("pt-BR")),
        )
    }
}
