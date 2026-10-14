package br.com.helpdev.lapscounter.model

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.helpdev.lapscounter.AppApplicationRealmConfiguration
import br.com.helpdev.lapscounter.model.entity.ActivityEntity
import br.com.helpdev.lapscounter.model.entity.ChronometerEntity
import br.com.helpdev.lapscounter.model.entity.LapEntity
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@RunWith(AndroidJUnit4::class)
class RealmUpgradeTest {

    @Test
    fun schemaVersionZeroPreservesSavedActivityAndOrderedLaps() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Realm.init(context)
        val fileName = "realm-upgrade-${System.nanoTime()}.realm"
        val oldConfiguration = RealmConfiguration.Builder()
            .directory(context.filesDir)
            .name(fileName)
            .schemaVersion(0)
            .build()

        Realm.getInstance(oldConfiguration).use { realm ->
            realm.executeTransaction {
                it.copyToRealm(
                    ActivityEntity(
                        id = "activity-upgrade-fixture",
                        name = "Morning swim",
                        description = "Pool session",
                        chronometer = ChronometerEntity(
                            pausedTime = 300,
                            runningTime = 12_000,
                            laps = RealmList(
                                LapEntity(4_000, 3_800, 100),
                                LapEntity(8_000, 7_600, 200),
                                LapEntity(12_000, 11_400, 300)
                            )
                        ),
                        lapDistance = 50f,
                        travelledDistance = 150f,
                        countLastLap = true,
                        dateStarted = Date(1_700_000_000_000)
                    )
                )
            }
        }

        Realm.getInstance(AppApplicationRealmConfiguration.create(context, fileName)).use { realm ->
            val activity = realm.where(ActivityEntity::class.java)
                .equalTo("id", "activity-upgrade-fixture")
                .findFirst()

            assertNotNull(activity)
            assertEquals("Morning swim", activity!!.name)
            assertEquals("Pool session", activity.description)
            assertEquals(50f, activity.lapDistance)
            assertEquals(150f, activity.travelledDistance)
            assertEquals(true, activity.countLastLap)
            assertEquals(1_700_000_000_000L, activity.dateStarted!!.time)
            assertEquals(12_000L, activity.chronometer!!.runningTime)
            assertEquals(300L, activity.chronometer!!.pausedTime)
            assertEquals(listOf(4_000L, 8_000L, 12_000L), activity.chronometer!!.laps.map { it.chronometerTime })
        }

        Realm.deleteRealm(oldConfiguration)
    }
}
