package br.com.helpdev.lapscounter.sync

import br.com.helpdev.lapscounter.model.entity.ActivityEntity
import br.com.helpdev.lapscounter.model.entity.ChronometerEntity
import br.com.helpdev.lapscounter.model.entity.LapEntity
import io.realm.RealmList
import java.util.Date

object ActivitySyncImporter {
    fun toActivityEntity(record: ActivitySyncRecord, expectedSportId: String): ActivityEntity {
        require(record.sportId == expectedSportId) { "Activity belongs to another sport" }
        // Apply the same wire-level bounds even for callers supplying a DTO directly.
        ActivitySyncCodec.encode(record)
        var elapsed = 0L
        val laps = RealmList<LapEntity>()
        record.laps.forEachIndexed { index, lap ->
            require(lap.index == index + 1) { "Laps must be ordered and contiguous" }
            require(!lap.isFinalPartialLap || index == record.laps.lastIndex)
            require(lap.elapsedMillis >= elapsed && lap.elapsedMillis - elapsed == lap.durationMillis)
            laps.add(LapEntity(elapsed, lap.durationMillis, 0))
            elapsed = lap.elapsedMillis
        }
        require(elapsed == record.runningTimeMillis) { "Lap times do not match activity running time" }
        return ActivityEntity(
            id = record.activityId,
            name = record.name,
            description = record.description,
            chronometer = ChronometerEntity(record.pausedTimeMillis, record.runningTimeMillis, laps),
            lapDistance = record.lapDistanceMeters,
            travelledDistance = record.travelledDistanceMeters,
            countLastLap = record.countLastLap,
            dateStarted = Date(record.startTimestampMillis)
        )
    }
}
