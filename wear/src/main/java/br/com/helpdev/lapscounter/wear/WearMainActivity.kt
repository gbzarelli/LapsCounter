package br.com.helpdev.lapscounter.wear

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.wear.compose.material3.MaterialTheme
import br.com.helpdev.lapscounter.wear.data.WatchActivityRepository
import br.com.helpdev.lapscounter.wear.data.WatchDatabase
import br.com.helpdev.lapscounter.wear.notifications.WearOngoingActivityIndicator
import br.com.helpdev.lapscounter.wear.sync.*
import br.com.helpdev.lapscounter.wear.timer.bootAwareElapsedRealtimeClock
import br.com.helpdev.lapscounter.wear.ui.WearApp
import br.com.helpdev.lapscounter.wear.ui.WearViewModel

class WearMainActivity : ComponentActivity() {
    private lateinit var model: WearViewModel
    private val notificationPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && WearOngoingActivityIndicator.canPostNotifications(this)) {
            model.setNotificationPermissionRequired(false)
            model.start()
        } else {
            model.setNotificationPermissionRequired(true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val database = WatchDatabase.open(applicationContext)
                val repository = WatchActivityRepository(database)
                val coordinator = createActivitySyncCoordinator(applicationContext, repository)
                @Suppress("UNCHECKED_CAST")
                return WearViewModel(
                    repository, resources, clock = bootAwareElapsedRealtimeClock(applicationContext),
                    closeDatabase = database::close, retrySync = coordinator::recover,
                    watchActivityIndicator = WearOngoingActivityIndicator(applicationContext),
                ) as T
            }
        })[WearViewModel::class.java]
        setContent { MaterialTheme { WearApp(model, ::startTimerWithNotificationAccess, ::openNotificationSettings) } }
    }

    private fun startTimerWithNotificationAccess() {
        if (WearOngoingActivityIndicator.canPostNotifications(this)) {
            model.setNotificationPermissionRequired(false)
            model.start()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            model.setNotificationPermissionRequired(true)
        }
    }

    private fun openNotificationSettings() {
        startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        )
    }

    override fun onResume() {
        super.onResume()
        if (model.state.value.notificationPermissionRequired && WearOngoingActivityIndicator.canPostNotifications(this)) {
            model.setNotificationPermissionRequired(false)
        }
        model.onAppResumed()
    }
}
