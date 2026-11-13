package br.com.helpdev.lapscounter.wear.data

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import br.com.helpdev.lapscounter.wear.timer.WatchSessionState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class WatchDatabaseMigrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test fun versionOneMigrationPreservesActiveHistoryLapsAndSettings() = runBlocking {
        val name = "watch-migration-test.db"
        val context = instrumentation.targetContext
        context.deleteDatabase(name)
        // Load the committed v1 schema independently of the v2 entities/migration.
        val schema = org.json.JSONObject(instrumentation.context.assets.open(
            "br.com.helpdev.lapscounter.wear.data.WatchDatabase/1.json").bufferedReader().use { it.readText() })
            .getJSONObject("database")
        val old = FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name).callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    val entities = schema.getJSONArray("entities")
                    for (index in 0 until entities.length()) {
                        val entity = entities.getJSONObject(index)
                        db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
                        val indices = entity.optJSONArray("indices") ?: org.json.JSONArray()
                        for (i in 0 until indices.length()) db.execSQL(indices.getJSONObject(i).getString("createSql")
                            .replace("\${TABLE_NAME}", entity.getString("tableName")))
                    }
                    val setup = schema.getJSONArray("setupQueries")
                    for (index in 0 until setup.length()) db.execSQL(setup.getString(index))
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = error("Unexpected upgrade")
            }).build())
        old.writableDatabase.apply {
            execSQL("INSERT INTO watch_activities VALUES ('active', 'swimming', 123, 1000, 6000, NULL, 1000, 4000, 'PAUSED', 50, 0, 'PENDING', 1, '', NULL)")
            execSQL("INSERT INTO watch_activities VALUES ('saved', 'swimming', 122, 1000, NULL, 5000, 0, 4000, 'STOPPED', 50, 1, 'SYNCED', 0, 'Morning', 'Pool')")
            execSQL("INSERT INTO watch_laps VALUES ('active', 1, 3000, 3000, 0)")
            execSQL("INSERT INTO watch_laps VALUES ('saved', 1, 4000, 4000, 1)")
            execSQL("INSERT INTO wear_settings VALUES ('swimming', 75, 0)")
        }
        old.close()
        val database = Room.databaseBuilder(context, WatchDatabase::class.java, name)
            .addMigrations(WatchDatabase.MIGRATION_1_2).build()
        try {
            val repository = WatchActivityRepository(database)
            val active = repository.loadActive()!!
            assertEquals("active", active.id)
            assertEquals(WatchSessionState.PAUSED, active.state)
            assertEquals(4000L, active.elapsedMillis)
            assertEquals(1000L, active.accumulatedPausedMillis)
            assertEquals(6000L, active.pauseElapsedRealtimeMillis)
            assertEquals(listOf(3000L), active.laps.map { it.durationMillis })
            assertNull(active.bootIdentity)
            assertNull(active.checkpointRealtimeMillis)
            val saved = repository.history().single()
            assertEquals("saved", saved.activity.id)
            assertEquals("Morning", saved.activity.name)
            assertEquals("Pool", saved.activity.description)
            assertEquals(WatchSyncStatus.SYNCED, saved.activity.syncStatus)
            assertEquals(4000L, saved.activity.elapsedMillis)
            assertEquals(listOf(4000L), saved.laps.map { it.durationMillis })
            assertTrue(saved.laps.single().isFinalPartialLap)
            assertEquals(WearSettingsEntity("swimming", 75f, false), repository.settings("swimming"))
        } finally { database.close(); context.deleteDatabase(name) }
    }
}
