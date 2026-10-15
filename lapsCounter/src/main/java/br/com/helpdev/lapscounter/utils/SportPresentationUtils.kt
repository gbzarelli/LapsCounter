package br.com.helpdev.lapscounter.utils

import android.content.Context
import br.com.helpdev.lapscounter.R
import br.com.helpdev.lapscounter.sport.MetricDefinition
import br.com.helpdev.lapscounter.sport.SportMetricLabels
import br.com.helpdev.lapscounter.sport.SportMetricsPresentation
import br.com.helpdev.lapscounter.sport.SportMetricsPresenter
import br.com.helpdev.lapscounter.sport.SportProfile
import br.com.helpdev.lapscounter.sport.SportProfileProvider
import java.util.Locale

fun Context.presentSportMetrics(
    runningTimeMillis: Long,
    splitDistanceMeters: Float,
    travelledDistanceMeters: Float,
    profile: SportProfile = SportProfileProvider.get(),
): SportMetricsPresentation = SportMetricsPresenter.present(
    runningTimeMillis = runningTimeMillis,
    splitDistanceMeters = splitDistanceMeters,
    travelledDistanceMeters = travelledDistanceMeters,
    profile = profile,
    labels = sportMetricLabels(profile),
    locale = sportLocale(),
)

fun Context.formatSportMetric(
    runningTimeMillis: Long,
    distanceMeters: Float,
    profile: SportProfile = SportProfileProvider.get(),
): String = SportMetricsPresenter.formatMetric(
    runningTimeMillis = runningTimeMillis,
    distanceMeters = distanceMeters,
    profile = profile,
    labels = sportMetricLabels(profile),
    locale = sportLocale(),
)

private fun Context.sportLocale() = resources.configuration.locales[0] ?: Locale.getDefault()

fun Context.sportMetricLabels(profile: SportProfile = SportProfileProvider.get()) = SportMetricLabels(
    paceReferenceUnit = getString(
        when (profile.metricDefinition) {
            is MetricDefinition.Pace -> if (profile.metricDefinition.referenceDistanceMeters == 100f) {
                R.string.metric_pace_reference_100m
            } else {
                R.string.metric_pace_reference_km
            }

            MetricDefinition.AverageSpeed -> R.string.metric_pace_reference_km
        },
    ),
    speedUnit = getString(R.string.metric_speed_unit),
    unavailable = getString(R.string.metric_unavailable),
)

fun Context.sportMetricTitle(profile: SportProfile = SportProfileProvider.get()): String = getString(
    when (profile.metricDefinition) {
        is MetricDefinition.Pace -> R.string.metric_label_pace
        MetricDefinition.AverageSpeed -> R.string.metric_label_average_speed
    },
)
