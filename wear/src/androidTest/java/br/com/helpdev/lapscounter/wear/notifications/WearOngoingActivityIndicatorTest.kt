package br.com.helpdev.lapscounter.wear.notifications

import android.Manifest
import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.wear.ongoing.OngoingActivity
import br.com.helpdev.lapscounter.wear.timer.ElapsedRealtimeClock
import br.com.helpdev.lapscounter.wear.timer.WatchTimerController
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WearOngoingActivityIndicatorTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val notificationManager = NotificationManagerCompat.from(context)
    private lateinit var indicator: WearOngoingActivityIndicator

    @Before fun setUp() {
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
            context.packageName,
            Manifest.permission.POST_NOTIFICATIONS,
        )
        indicator = WearOngoingActivityIndicator(context)
    }

    @After fun tearDown() {
        indicator.cancel()
    }

    @Test fun postsUpdatesAndCancelsARealOngoingNotification() {
        assertTrue(WearOngoingActivityIndicator.canPostNotifications(context))
        var now = 1_000L
        val timer = WatchTimerController(
            clock = ElapsedRealtimeClock { now },
            flavorId = "swimming",
            lapDistanceMeters = 25f,
            wallClockMillis = { 1_735_732_800_000L },
        )

        indicator.update(timer.start())
        var notification = notificationManager.activeNotifications.single().notification
        assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertEquals(Notification.CATEGORY_STOPWATCH, notification.category)
        assertNotNull(OngoingActivity.recoverOngoingActivity(context))

        now = 61_000L
        indicator.update(timer.pause())
        notification = notificationManager.activeNotifications.single().notification
        assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertNotNull(OngoingActivity.recoverOngoingActivity(context))

        indicator.cancel()
        assertTrue(notificationManager.activeNotifications.isEmpty())
        assertNull(OngoingActivity.recoverOngoingActivity(context))
    }
}
