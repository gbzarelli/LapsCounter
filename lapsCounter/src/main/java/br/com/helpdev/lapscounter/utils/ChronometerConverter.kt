package br.com.helpdev.lapscounter.utils

import br.com.helpdev.chronometerlib.Chronometer
import br.com.helpdev.lapscounter.model.entity.ActivityEntity
import br.com.helpdev.lapscounter.model.entity.LapEntity

fun Chronometer.toActivityEntity(
    name: String,
    description: String,
    lapDistance: Float
): ActivityEntity {
    val activityEntity = ActivityEntity()
    activityEntity.name = name
    activityEntity.description = description
    activityEntity.lapDistance = lapDistance
    activityEntity.dateStarted = this.dateTime

    for (x in 0 until getLaps().size) {
        val lap = getLaps()[x]
        activityEntity.chronometer!!.laps.add(
            LapEntity(
                lap.accumulatedStartTime,
                lap.getRunTime(),
                lap.pausedTime
            )
        )
        //TODO CAN BE REFACTOR
        activityEntity.chronometer!!.runningTime += lap.getRunTime()
        activityEntity.chronometer!!.pausedTime += lap.pausedTime
    }

    return activityEntity
}