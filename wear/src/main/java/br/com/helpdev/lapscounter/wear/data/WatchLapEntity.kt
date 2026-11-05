package br.com.helpdev.lapscounter.wear.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "watch_laps",
    primaryKeys = ["activityId", "sequence"],
    foreignKeys = [ForeignKey(
        entity = WatchActivityEntity::class,
        parentColumns = ["id"],
        childColumns = ["activityId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("activityId")],
)
data class WatchLapEntity(
    val activityId: String,
    val sequence: Int,
    val durationMillis: Long,
    val elapsedMillis: Long,
    val isFinalPartialLap: Boolean,
)
