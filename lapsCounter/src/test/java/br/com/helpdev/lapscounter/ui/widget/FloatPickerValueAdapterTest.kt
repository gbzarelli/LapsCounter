package br.com.helpdev.lapscounter.ui.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class FloatPickerValueAdapterTest {
    @Test
    fun convertsFractionalPickerDigitsAsHundredths() {
        assertEquals(1.05f, FloatPickerValueAdapter.toFloat(1, 5), 0f)
        assertEquals(1.5f, FloatPickerValueAdapter.toFloat(1, 50), 0f)
    }

    @Test
    fun displaysFractionalPickerDigitsWithLeadingZeroes() {
        val displayedValues = FloatPickerValueAdapter.fractionalDisplayedValues()

        assertEquals("05", displayedValues[5])
        assertEquals("50", displayedValues[50])
    }
}
