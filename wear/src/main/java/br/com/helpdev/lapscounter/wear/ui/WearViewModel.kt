package br.com.helpdev.lapscounter.wear.ui

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.helpdev.lapscounter.sport.*
import br.com.helpdev.lapscounter.wear.R
import br.com.helpdev.lapscounter.wear.data.*
import br.com.helpdev.lapscounter.wear.timer.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** All mutations and their durable acknowledgements share one coroutine lock. */
class WearViewModel(
    private val repository: WatchActivityRepository,
    private val resources: Resources,
    private val clock: ElapsedRealtimeClock = AndroidElapsedRealtimeClock,
    private val wallClock: () -> Long = System::currentTimeMillis,
    private val closeDatabase: () -> Unit = {},
) : ViewModel() {
    val profile = SportProfileProvider.get()
    val flavorId = profile.sportId.name.lowercase(Locale.ROOT)
    private val mutex = Mutex()
    private var controller: WatchTimerController? = null
    private val mutableState = MutableStateFlow(WearUiState())
    val state: StateFlow<WearUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            operation {
                val settings = repository.settings(flavorId)
                    ?: WearSettingsEntity(flavorId, profile.defaultLapDistanceMeters, true)
                val restored = repository.loadActive()
                controller = timer(settings, restored)
                mutableState.value = mutableState.value.copy(
                    ready = true, settings = settings, history = repository.history(),
                )
                display()
            }
            while (true) {
                delay(200)
                mutex.withLock { if (state.value.ready) display() }
            }
        }
    }

    private fun timer(settings: WearSettingsEntity, restored: WatchSessionSnapshot? = null) = WatchTimerController(
        clock, flavorId, settings.lapDistanceMeters, settings.countLastLap, restored, wallClock,
    )

    private suspend fun operation(block: suspend () -> Unit) = mutex.withLock {
        mutableState.value = state.value.copy(busy = true, error = false)
        try {
            block()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            mutableState.value = state.value.copy(error = true)
        } finally {
            mutableState.value = state.value.copy(busy = false)
        }
    }

    private fun transition(expected: Set<WatchSessionState>, change: WatchTimerController.() -> WatchSessionSnapshot) {
        viewModelScope.launch {
            operation {
                val current = controller ?: return@operation
                val previous = current.snapshot()
                if (previous.state !in expected) return@operation
                try {
                    repository.persistActive(current.change())
                } catch (error: Exception) {
                    controller = timer(requireNotNull(state.value.settings), previous)
                    throw error
                }
                display()
            }
        }
    }

    fun start() = transition(setOf(WatchSessionState.IDLE)) { start() }
    fun lap() = transition(setOf(WatchSessionState.RUNNING)) { recordLap() }
    fun pause() = transition(setOf(WatchSessionState.RUNNING)) { pause() }
    fun resume() = transition(setOf(WatchSessionState.PAUSED)) { resume() }
    fun stop() = transition(setOf(WatchSessionState.RUNNING, WatchSessionState.PAUSED)) { stop() }

    fun discard() {
        viewModelScope.launch {
            operation {
                val current = controller ?: return@operation
                val previous = current.snapshot()
                if (previous.state != WatchSessionState.STOPPED) return@operation
                try {
                    repository.persistActive(current.discard())
                } catch (error: Exception) {
                    controller = timer(requireNotNull(state.value.settings), previous)
                    throw error
                }
                reset()
            }
        }
    }

    fun save() {
        viewModelScope.launch {
            operation {
                val snapshot = controller?.snapshot() ?: return@operation
                if (snapshot.state != WatchSessionState.STOPPED) return@operation
                repository.saveCompleted(snapshot, generatedName(snapshot))
                mutableState.value = state.value.copy(history = repository.history())
                reset()
            }
        }
    }

    fun saveSettings(distance: Float, countLastLap: Boolean, onSaved: () -> Unit) {
        viewModelScope.launch {
            operation {
                val settings = WearSettingsEntity(flavorId, distance, countLastLap)
                repository.saveSettings(settings)
                mutableState.value = state.value.copy(settings = settings)
                if (controller?.snapshot()?.state == WatchSessionState.IDLE) reset()
                onSaved()
            }
        }
    }

    fun loadActivity(id: String) {
        viewModelScope.launch {
            operation { mutableState.value = state.value.copy(detail = repository.activity(id)) }
        }
    }

    private fun reset() {
        controller = timer(requireNotNull(state.value.settings))
        display()
    }

    private fun display() {
        val snapshot = controller?.snapshot() ?: return
        mutableState.value = state.value.copy(
            snapshot = snapshot, metrics = metrics(snapshot),
            generatedName = snapshot.startWallClockMillis?.let { generatedName(snapshot) }.orEmpty(),
            syncStatus = snapshot.syncStatus,
        )
    }

    fun metrics(snapshot: WatchSessionSnapshot): SportMetricsPresentation = SportMetricsPresenter.present(
        snapshot.elapsedMillis, snapshot.lapDistanceMeters, snapshot.distanceMeters, profile,
        SportMetricLabels(resources.getString(R.string.pace_reference_unit), "km/h", "—"),
        resources.configuration.locales[0],
    )

    private fun generatedName(snapshot: WatchSessionSnapshot): String {
        val date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, resources.configuration.locales[0])
            .format(Date(requireNotNull(snapshot.startWallClockMillis)))
        return resources.getString(R.string.activity_generated_name, resources.getString(R.string.sport_name), date)
    }

    override fun onCleared() { closeDatabase() }
}

data class WearUiState(
    val ready: Boolean = false,
    val busy: Boolean = false,
    val error: Boolean = false,
    val snapshot: WatchSessionSnapshot? = null,
    val metrics: SportMetricsPresentation? = null,
    val generatedName: String = "",
    val settings: WearSettingsEntity? = null,
    val history: List<WatchActivityRecord> = emptyList(),
    val detail: WatchActivityRecord? = null,
    val syncStatus: WatchSyncStatus = WatchSyncStatus.PENDING,
)
