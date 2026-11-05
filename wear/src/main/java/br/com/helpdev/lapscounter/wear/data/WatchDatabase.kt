package br.com.helpdev.lapscounter.wear.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [WatchActivityEntity::class, WatchLapEntity::class, WearSettingsEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class WatchDatabase : RoomDatabase() {
    abstract fun activityDao(): WatchActivityDao

    companion object {
        fun open(context: Context): WatchDatabase = Room.databaseBuilder(
            context.applicationContext,
            WatchDatabase::class.java,
            "watch-activities.db",
        ).build()
    }
}
