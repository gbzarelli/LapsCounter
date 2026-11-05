package br.com.helpdev.lapscounter.wear.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wear_settings")
data class WearSettingsEntity(
    @PrimaryKey val flavorId: String,
    val lapDistanceMeters: Float,
    val countLastLap: Boolean = true,
)
