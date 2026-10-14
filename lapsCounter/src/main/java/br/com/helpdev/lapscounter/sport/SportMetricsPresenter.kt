package br.com.helpdev.lapscounter.sport

import java.util.Locale

data class SportMetricLabels(
    val paceReferenceUnit: String,
    val speedUnit: String,
    val unavailable: String,
)

data class SportMetricsPresentation(
    val splitDistance: String,
    val travelledDistance: String,
    val metric: String,
)

object SportMetricsPresenter {
    fun present(
        runningTimeMillis: Long,
        splitDistanceMeters: Float,
        travelledDistanceMeters: Float,
        profile: SportProfile,
        labels: SportMetricLabels,
        locale: Locale,
    ): SportMetricsPresentation {
        return SportMetricsPresentation(
            splitDistance = SportDistanceFormatter.format(
                splitDistanceMeters,
                profile.distanceDisplayUnit,
                locale,
            ),
            travelledDistance = SportDistanceFormatter.format(
                travelledDistanceMeters,
                profile.distanceDisplayUnit,
                locale,
            ),
            metric = formatMetric(
                runningTimeMillis = runningTimeMillis,
                distanceMeters = travelledDistanceMeters,
                profile = profile,
                labels = labels,
                locale = locale,
            ),
        )
    }

    fun formatMetric(
        runningTimeMillis: Long,
        distanceMeters: Float,
        profile: SportProfile,
        labels: SportMetricLabels,
        locale: Locale,
    ): String = SportMetricFormatter.format(
        metric = SportMetricCalculator.calculate(
            runningTimeMillis = runningTimeMillis,
            distanceMeters = distanceMeters,
            definition = profile.metricDefinition,
        ),
        paceUnitLabel = labels.paceReferenceUnit,
        speedUnitLabel = labels.speedUnit,
        unavailableLabel = labels.unavailable,
        locale = locale,
    )
}
