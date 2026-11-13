package br.com.helpdev.lapscounter.wear.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [WatchActivityEntity::class, WatchLapEntity::class, WearSettingsEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class WatchDatabase : RoomDatabase() {
    abstract fun activityDao(): WatchActivityDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE watch_activities ADD COLUMN bootIdentity TEXT")
                db.execSQL("ALTER TABLE watch_activities ADD COLUMN checkpointRealtimeMillis INTEGER")
            }
        }

        fun open(context: Context): WatchDatabase = Room.databaseBuilder(
            context.applicationContext,
            WatchDatabase::class.java,
            "watch-activities.db",
        ).addMigrations(MIGRATION_1_2).enableMultiInstanceInvalidation().build()
    }
}
