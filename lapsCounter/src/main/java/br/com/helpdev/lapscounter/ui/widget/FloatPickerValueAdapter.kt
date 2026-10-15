package br.com.helpdev.lapscounter.ui.widget

internal object FloatPickerValueAdapter {
    fun toFloat(wholeUnits: Int, hundredths: Int): Float = wholeUnits + hundredths / 100f

    fun fractionalDisplayedValues(): Array<String> = Array(100) { value ->
        value.toString().padStart(2, '0')
    }
}
