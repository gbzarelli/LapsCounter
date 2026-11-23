package br.com.helpdev.lapscounter.wear.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import br.com.helpdev.lapscounter.wear.R
import br.com.helpdev.lapscounter.wear.WearMainActivity
import br.com.helpdev.lapscounter.wear.timer.WatchSessionSnapshot
import br.com.helpdev.lapscounter.wear.timer.WatchSessionState

interface WatchActivityIndicator {
    fun update(snapshot: WatchSessionSnapshot)
    fun cancel()
}

/** Keeps the active watch timer available from the watch face and launcher Recents. */
class WearOngoingActivityIndicator(context: Context) : WatchActivityIndicator {
    private val appContext = context.applicationContext
    private val notificationManager = NotificationManagerCompat.from(appContext)
    private var ongoingActivity: OngoingActivity? = null

    init {
        val nativeManager = appContext.getSystemService(NotificationManager::class.java)
        nativeManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                appContext.getString(R.string.ongoing_notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    override fun update(snapshot: WatchSessionSnapshot) {
        check(snapshot.state == WatchSessionState.RUNNING || snapshot.state == WatchSessionState.PAUSED)
        if (!canPostNotifications(appContext)) throw SecurityException("Notification access is required for an active timer.")

        val status = status(snapshot)
        val active = ongoingActivity ?: OngoingActivity.recoverOngoingActivity(appContext)
        if (active != null) {
            active.update(appContext, status)
            ongoingActivity = active
            return
        }

        val touchIntent = PendingIntent.getActivity(
            appContext,
            NOTIFICATION_ID,
            Intent(appContext, WearMainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_ongoing_stopwatch)
            .setContentTitle(appContext.getString(R.string.sport_name))
            .setContentText(appContext.getString(R.string.ongoing_notification_text))
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setContentIntent(touchIntent)

        val activity = OngoingActivity.Builder(appContext, NOTIFICATION_ID, notification)
            .setStaticIcon(R.drawable.ic_ongoing_stopwatch)
            .setTouchIntent(touchIntent)
            .setTitle(appContext.getString(R.string.sport_name))
            .setContentDescription(appContext.getString(R.string.ongoing_accessibility, appContext.getString(R.string.sport_name)))
            .setStatus(status)
            .build()
        activity.apply(appContext)
        notificationManager.notify(NOTIFICATION_ID, notification.build())
        ongoingActivity = activity
    }

    override fun cancel() {
        notificationManager.cancel(NOTIFICATION_ID)
        ongoingActivity = null
    }

    private fun status(snapshot: WatchSessionSnapshot): Status = when (snapshot.state) {
        WatchSessionState.RUNNING -> Status.Builder()
            .addTemplate("#sport# #timer#")
            .addPart("sport", Status.TextPart(appContext.getString(R.string.sport_name)))
            .addPart(
                "timer",
                Status.StopwatchPart(SystemClock.elapsedRealtime() - snapshot.elapsedMillis),
            )
            .build()
        WatchSessionState.PAUSED -> Status.Builder()
            .addTemplate("#sport# #status#")
            .addPart("sport", Status.TextPart(appContext.getString(R.string.sport_name)))
            .addPart("status", Status.TextPart(appContext.getString(R.string.ongoing_paused, elapsedLabel(snapshot.elapsedMillis))))
            .build()
        else -> error("Only active timer states have an ongoing status.")
    }

    private fun elapsedLabel(millis: Long): String {
        val seconds = millis / 1_000
        return "%02d:%02d:%02d".format(seconds / 3_600, seconds / 60 % 60, seconds % 60)
    }

    companion object {
        private const val CHANNEL_ID = "active_stopwatch"
        private const val NOTIFICATION_ID = 7_103

        fun canPostNotifications(context: Context): Boolean {
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return false
            val manager = context.getSystemService(NotificationManager::class.java)
            return manager.getNotificationChannel(CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE
        }
    }
}
