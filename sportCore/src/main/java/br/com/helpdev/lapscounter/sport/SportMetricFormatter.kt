package br.com.helpdev.lapscounter.sport

import java.text.NumberFormat
import java.util.Locale

object SportMetricFormatter {
    fun format(
        metric: SportMetricResult,
        paceUnitLabel: String,
        speedUnitLabel: String,
        unavailableLabel: String,
        locale: Locale,
    ): String = when (metric) {
        is SportMetricResult.Pace -> {
            val minutes = metric.totalSeconds / 60
            val seconds = metric.totalSeconds % 60
            String.format(locale, "%d:%02d / %s", minutes, seconds, paceUnitLabel)
        }

        is SportMetricResult.AverageSpeed -> {
            val numberFormat = NumberFormat.getNumberInstance(locale).apply {
                isGroupingUsed = false
                minimumFractionDigits = 1
                maximumFractionDigits = 1
            }
            "${numberFormat.format(metric.kilometresPerHour)} $speedUnitLabel"
        }

        SportMetricResult.Unavailable -> unavailableLabel
    }
}
