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
        val metric = SportMetricCalculator.calculate(
            runningTimeMillis = runningTimeMillis,
            distanceMeters = travelledDistanceMeters,
            definition = profile.metricDefinition,
        )

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
            metric = SportMetricFormatter.format(
                metric = metric,
                paceUnitLabel = labels.paceReferenceUnit,
                speedUnitLabel = labels.speedUnit,
                unavailableLabel = labels.unavailable,
                locale = locale,
            ),
        )
    }
}
