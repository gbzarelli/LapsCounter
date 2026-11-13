package br.com.helpdev.lapscounter.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.wear.compose.material3.MaterialTheme
import br.com.helpdev.lapscounter.wear.data.WatchActivityRepository
import br.com.helpdev.lapscounter.wear.data.WatchDatabase
import br.com.helpdev.lapscounter.wear.sync.*
import br.com.helpdev.lapscounter.wear.timer.bootAwareElapsedRealtimeClock
import br.com.helpdev.lapscounter.wear.ui.WearApp
import br.com.helpdev.lapscounter.wear.ui.WearViewModel

class WearMainActivity : ComponentActivity() {
    private lateinit var model: WearViewModel
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
                ) as T
            }
        })[WearViewModel::class.java]
        setContent { MaterialTheme { WearApp(model) } }
    }
    override fun onResume() {
        super.onResume()
        model.onAppResumed()
    }
}
