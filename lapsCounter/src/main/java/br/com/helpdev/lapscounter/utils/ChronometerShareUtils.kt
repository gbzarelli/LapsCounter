package br.com.helpdev.lapscounter.utils

import android.content.Context
import android.content.Intent
import br.com.helpdev.chronometerlib.Chronometer
import br.com.helpdev.chronometerlib.ChronometerUtils
import br.com.helpdev.lapscounter.R
import br.com.helpdev.lapscounter.model.entity.ActivityEntity
import br.com.helpdev.lapscounter.sport.SportProfileProvider
import java.text.SimpleDateFormat
import java.util.Locale

object ChronometerShareUtils {
    fun shareText(
        context: Context,
        chronometer: Chronometer,
        lapDistance: Float,
        countLastLap: Boolean,
    ) {
        val profile = SportProfileProvider.get()
        var pausedTime = 0L
        var totalRunningTime = 0L
        val laps = StringBuilder()

        for ((index, lap) in chronometer.getLaps().withIndex()) {
            pausedTime += lap.pausedTime
            val lapRunningTime = lap.getRunTime().coerceAtLeast(0L)
            totalRunningTime += lapRunningTime
            val lapMetric = context.formatSportMetric(
                runningTimeMillis = lap.getRunTime(),
                distanceMeters = lapDistance,
                profile = profile,
            )

            laps.append(
                context.getString(
                    R.string.text_lap_share,
                    index + 1,
                    ChronometerUtils.getFormattedTime(lap.getRunTime()),
                    ChronometerUtils.getFormattedTime(lap.getAccumulatedTime()),
                    ChronometerUtils.getFormattedTime(lap.pausedTime),
                    context.sportMetricTitle(profile),
                    lapMetric,
                ),
            )
        }

        val countedDistance = LapsCounterUtils.getDistanceTravelled(chronometer, lapDistance, countLastLap)
        val presentation = context.presentSportMetrics(
            runningTimeMillis = LapsCounterUtils.getRunningTime(chronometer, countLastLap),
            splitDistanceMeters = lapDistance,
            travelledDistanceMeters = countedDistance,
            profile = profile,
        )

        if (!countLastLap) {
            laps.append(context.getString(R.string.last_lap_dont_count)).append("\n")
        }

        val stringShare = context.getString(
            R.string.text_to_share,
            simpleDateFormat().format(chronometer.dateTime),
            ChronometerUtils.getFormattedTime(totalRunningTime),
            ChronometerUtils.getFormattedTime(pausedTime),
            ChronometerUtils.getFormattedTime(totalRunningTime + pausedTime),
            presentation.splitDistance,
            presentation.travelledDistance,
            context.sportMetricTitle(profile),
            presentation.metric,
            laps.toString(),
        )

        shareText(context, context.getString(R.string.app_name), stringShare)
    }

    fun shareText(context: Context, activityEntity: ActivityEntity) {
        val profile = SportProfileProvider.get()
        var pausedTime = 0L
        var totalRunningTime = 0L
        val laps = StringBuilder()
        val savedLaps = activityEntity.chronometer?.laps.orEmpty()

        for ((index, lap) in savedLaps.withIndex()) {
            pausedTime += lap.pausedTime
            totalRunningTime += lap.runningTime
            val lapMetric = context.formatSportMetric(
                runningTimeMillis = lap.runningTime,
                distanceMeters = activityEntity.lapDistance,
                profile = profile,
            )

            laps.append(
                context.getString(
                    R.string.text_lap_share,
                    index + 1,
                    ChronometerUtils.getFormattedTime(lap.runningTime),
                    ChronometerUtils.getFormattedTime(lap.chronometerTime),
                    ChronometerUtils.getFormattedTime(lap.pausedTime),
                    context.sportMetricTitle(profile),
                    lapMetric,
                ),
            )
        }

        val presentation = context.presentSportMetrics(
            runningTimeMillis = LapsCounterUtils.getRunningTime(activityEntity),
            splitDistanceMeters = activityEntity.lapDistance,
            travelledDistanceMeters = activityEntity.travelledDistance,
            profile = profile,
        )

        if (!activityEntity.countLastLap) {
            laps.append(context.getString(R.string.last_lap_dont_count)).append("\n")
        }

        val stringShare = context.getString(
            R.string.text_to_share,
            simpleDateFormat().format(activityEntity.dateStarted),
            ChronometerUtils.getFormattedTime(totalRunningTime),
            ChronometerUtils.getFormattedTime(pausedTime),
            ChronometerUtils.getFormattedTime(totalRunningTime + pausedTime),
            presentation.splitDistance,
            presentation.travelledDistance,
            context.sportMetricTitle(profile),
            presentation.metric,
            laps.toString(),
        )

        shareText(context, context.getString(R.string.app_name), stringShare)
    }

    private fun simpleDateFormat() = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    private fun shareText(context: Context, subject: String, body: String) {
        val txtIntent = Intent(Intent.ACTION_SEND)
        txtIntent.type = "text/plain"
        txtIntent.putExtra(Intent.EXTRA_SUBJECT, subject)
        txtIntent.putExtra(Intent.EXTRA_TEXT, body)
        context.startActivity(Intent.createChooser(txtIntent, "SHARE"))
    }
}
