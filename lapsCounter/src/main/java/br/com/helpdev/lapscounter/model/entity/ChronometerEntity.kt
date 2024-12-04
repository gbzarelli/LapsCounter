package br.com.helpdev.lapscounter.model.entity

import br.com.helpdev.chronometerlib.Chronometer
import io.realm.RealmList
import io.realm.RealmObject
import java.io.Serializable

fun ChronometerEntity.toObLaps(): List<Chronometer> {
    val obLaps = mutableListOf<Chronometer>()
    laps.forEach {
        obLaps.add(it.toObLap())
    }
    return obLaps
}

open class ChronometerEntity(
    var pausedTime: Long = 0,
    var runningTime: Long = 0,
    var laps: RealmList<LapEntity> = RealmList()
) : RealmObject(), Serializable {
    override fun toString() =
        "ChronometerEntity(pausedTime=$pausedTime, runningTime=$runningTime, laps=$laps)"
}