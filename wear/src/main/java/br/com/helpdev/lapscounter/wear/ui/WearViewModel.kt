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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** All mutations and their durable acknowledgements share one coroutine lock. */
class WearViewModel(
    private val repository: WatchActivityStore,
    private val resources: Resources,
    private val clock: ElapsedRealtimeClock = AndroidElapsedRealtimeClock,
    private val wallClock: () -> Long = System::currentTimeMillis,
    private val closeDatabase: () -> Unit = {},
    private val retrySync: suspend () -> Unit = {},
) : ViewModel() {
    val profile = SportProfileProvider.get()
    val flavorId = profile.sportId.name.lowercase(Locale.ROOT)
    private val mutex = Mutex()
    private val timerVisible = MutableStateFlow(false)
    private var controller: WatchTimerController? = null
    private var detailRequest = 0
    private var historyRequest = 0
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
                    ready = true, settings = settings,
                )
                display()
            }
        }
        viewModelScope.launch {
            combine(timerVisible, state) { visible, ui -> visible && ui.snapshot?.state == WatchSessionState.RUNNING }
                .distinctUntilChanged().collectLatest { runningVisible ->
                    if (runningVisible) while (true) {
                        delay(200)
                        mutex.withLock { display() }
                    }
                }
        }
        viewModelScope.launch {
            state.map { it.snapshot?.state in setOf(WatchSessionState.RUNNING, WatchSessionState.PAUSED) }
                .distinctUntilChanged().collectLatest { active ->
                    // Attempt a durable recovery boundary independently of display ticks,
                    // including while backgrounded. Only successful writes preserve time.
                    if (active) while (true) { delay(30_000); checkpoint() }
                }
        }
        viewModelScope.launch {
            repository.observeChanges().collect { refreshLocalRecords() }
        }
    }

    fun setTimerVisible(visible: Boolean) {
        timerVisible.value = visible
        if (!visible) viewModelScope.launch { checkpoint() }
    }

    private suspend fun checkpoint() = mutex.withLock {
        val snapshot = controller?.snapshot() ?: return@withLock
        if (snapshot.state !in setOf(WatchSessionState.RUNNING, WatchSessionState.PAUSED)) return@withLock
        try { repository.persistActive(snapshot) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { mutableState.value = state.value.copy(error = true) }
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
    fun finishAtCheckpoint() = transition(setOf(WatchSessionState.RECOVERY_REQUIRED)) { finishAtCheckpoint() }
    fun stop() = transition(setOf(WatchSessionState.RUNNING, WatchSessionState.PAUSED)) { stop() }

    fun discard() {
        viewModelScope.launch {
            operation {
                val current = controller ?: return@operation
                val previous = current.snapshot()
                if (previous.state !in setOf(WatchSessionState.STOPPED, WatchSessionState.RECOVERY_REQUIRED)) return@operation
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
                reset()
                requestSync()
            }
        }
    }

    fun onAppResumed() {
        refreshLocalRecords()
        requestSync()
    }

    private fun refreshLocalRecords() {
        if (state.value.history.isNotEmpty()) loadHistory()
        state.value.detailId?.let(::loadActivity)
    }

    private fun requestSync() {
        viewModelScope.launch {
            try { retrySync() } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) {
                // The durable pending rows are retried on connection or the next resume.
            }
            refreshLocalRecords()
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

    fun loadHistory() {
        val request = ++historyRequest
        mutableState.value = state.value.copy(historyLoading = true, historyError = false)
        viewModelScope.launch {
            try {
                val records = repository.history()
                if (request == historyRequest) mutableState.value = state.value.copy(history = records, historyLoading = false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (request == historyRequest) mutableState.value = state.value.copy(historyLoading = false, historyError = true)
            }
        }
    }

    fun loadActivity(id: String) {
        val request = ++detailRequest
        mutableState.value = state.value.copy(detailId = id, detail = null, detailError = false)
        viewModelScope.launch {
            try {
                val record = repository.activity(id)
                if (request == detailRequest) {
                    mutableState.value = state.value.copy(detail = record, detailError = record == null)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (request == detailRequest) mutableState.value = state.value.copy(detailError = true)
            }
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
    val historyLoading: Boolean = false,
    val historyError: Boolean = false,
    val detailId: String? = null,
    val detail: WatchActivityRecord? = null,
    val detailError: Boolean = false,
    val syncStatus: WatchSyncStatus = WatchSyncStatus.PENDING,
)
