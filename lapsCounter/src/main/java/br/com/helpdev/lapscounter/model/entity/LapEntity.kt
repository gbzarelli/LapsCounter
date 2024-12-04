package br.com.helpdev.lapscounter.model.entity

import br.com.helpdev.chronometerlib.Chronometer
import io.realm.RealmObject
import java.io.Serializable
import java.util.*

fun LapEntity.toObLap(): Chronometer {
    return Chronometer.Companion.Builder(
        Date(),
        chronometerTime - (runningTime + pausedTime),
        0L,
        runningTime + pausedTime,
        pausedTime
    ).build()
}

open class LapEntity(
    var chronometerTime: Long = 0,
    var runningTime: Long = 0,
    var pausedTime: Long = 0
) : RealmObject(), Serializable {
    override fun toString(): String {
        return "LapEntity(chronometerTime=$chronometerTime, runningTime=$runningTime, pausedTime=$pausedTime)"
    }
}