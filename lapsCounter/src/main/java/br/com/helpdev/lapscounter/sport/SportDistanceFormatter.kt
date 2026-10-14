package br.com.helpdev.lapscounter.sport

import java.text.NumberFormat
import java.util.Locale

object SportDistanceFormatter {
    fun format(distanceMeters: Float, unit: DistanceDisplayUnit, locale: Locale): String {
        val displayDistance = distanceMeters / unit.metersPerUnit
        val numberFormat = NumberFormat.getNumberInstance(locale).apply {
            isGroupingUsed = false
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }
        val unitLabel = when (unit) {
            DistanceDisplayUnit.METRES -> "m"
            DistanceDisplayUnit.KILOMETRES -> "km"
        }

        return "${numberFormat.format(displayDistance)} $unitLabel"
    }
}
