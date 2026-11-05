package br.com.helpdev.lapscounter.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.wear.compose.material3.MaterialTheme
import br.com.helpdev.lapscounter.wear.data.WatchActivityRepository
import br.com.helpdev.lapscounter.wear.data.WatchDatabase
import br.com.helpdev.lapscounter.wear.ui.WearApp
import br.com.helpdev.lapscounter.wear.ui.WearViewModel

class WearMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val model = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val database = WatchDatabase.open(applicationContext)
                @Suppress("UNCHECKED_CAST")
                return WearViewModel(WatchActivityRepository(database), resources, closeDatabase = database::close) as T
            }
        })[WearViewModel::class.java]
        setContent { MaterialTheme { WearApp(model) } }
    }
}
