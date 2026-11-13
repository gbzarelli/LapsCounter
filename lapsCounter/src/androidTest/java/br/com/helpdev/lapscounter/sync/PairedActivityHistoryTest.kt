package br.com.helpdev.lapscounter.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.helpdev.lapscounter.model.entity.ActivityEntity
import io.realm.Realm
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Run after the matching watch live test receives its ACK, with the same fresh UUID. */
@RunWith(AndroidJUnit4::class)
class PairedActivityHistoryTest {
    @Test fun acknowledgedWatchUuidAndLapsExistOnceInProductionPhoneHistory() {
        val id = InstrumentationRegistry.getArguments().getString("pairedActivityId")
        assumeTrue("Requires live pairedActivityId; see release guide", id != null)
        Realm.getDefaultInstance().use { realm ->
            val activities = realm.where(ActivityEntity::class.java).equalTo("id", id).findAll()
            assertEquals(1, activities.size)
            assertEquals(id, activities.single().id)
            assertEquals(listOf(4_000L, 3_000L), activities.single().chronometer!!.laps.map { it.runningTime })
        }
    }
}
