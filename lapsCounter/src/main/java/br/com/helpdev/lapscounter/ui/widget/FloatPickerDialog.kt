package br.com.helpdev.lapscounter.ui.widget

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.widget.NumberPicker
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import br.com.helpdev.lapscounter.R
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToInt

class FloatPickerDialog(context: Context, private val value: Float) : AlertDialog(context) {

    private var pk1: NumberPicker? = null
    private var pk2: NumberPicker? = null
    private lateinit var func: (Float) -> Unit

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.widget_float_picker_preference, null)
        setView(view)
        onBindDialogView(view)
        setMessage(context.getString(R.string.hint_input_lap_distance))
        setButton(DialogInterface.BUTTON_POSITIVE, context.getString(android.R.string.ok)) { _, _ ->
            val newValue = FloatPickerValueAdapter.toFloat(pk1?.value ?: 0, pk2?.value ?: 0)
            func(newValue)
        }
    }

    fun show(func: (Float) -> Unit) {
        this.func = func
        super.show()
    }

    fun onBindDialogView(view: View) {
        pk1 = view.findViewById(R.id.num_picker_1)
        pk2 = view.findViewById(R.id.num_picker_2)
        val decimalSeparator = view.findViewById<TextView>(R.id.decimal_separator)
        decimalSeparator.text = DecimalFormatSymbols.getInstance(
            context.resources.configuration.locales[0] ?: Locale.getDefault(),
        ).decimalSeparator.toString()
        if (null != pk1) {
            pk1!!.minValue = 0
            pk1!!.maxValue = 999
        }
        if (null != pk2) {
            pk2!!.minValue = 0
            pk2!!.maxValue = 99
            pk2!!.displayedValues = FloatPickerValueAdapter.fractionalDisplayedValues()
        }
        if (null != pk1 && null != pk2) {
            val valueHundredths = (value * 100).roundToInt().coerceAtLeast(0)
            pk1!!.value = valueHundredths / 100
            pk2!!.value = valueHundredths % 100
        }
    }
}
