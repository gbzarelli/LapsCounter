package br.com.helpdev.lapscounter

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import br.com.helpdev.lapscounter.headset.HeadsetButtonControl
import br.com.helpdev.lapscounter.injector.InjectorUtils
import br.com.helpdev.lapscounter.ui.ActivitiesActivity
import br.com.helpdev.lapscounter.ui.SettingsActivity
import br.com.helpdev.lapscounter.ui.adapter.LapsAdapter
import br.com.helpdev.lapscounter.ui.dialog.SaveActivityDialog
import br.com.helpdev.lapscounter.ui.viewmodel.ChronometerViewModel
import br.com.helpdev.lapscounter.ui.ads.AdsManager
import br.com.helpdev.lapscounter.databinding.ActivityMainBinding
import br.com.helpdev.lapscounter.sport.SportProfileProvider
import br.com.helpdev.chronometerlib.widget.ChronometerWidget
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import br.com.helpdev.lapscounter.utils.*
import br.com.helpdev.lapscounter.utils.AlarmUtils.alarmAsync
import kotlin.concurrent.thread

abstract class ChronometerActivity : AppCompatActivity(),
    HeadsetButtonControl.HeadsetButtonControlListener {
    companion object {
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: ChronometerViewModel

    private var headsetButtonReceiver: HeadsetButtonControl? = null

    private var dialog: AlertDialog? = null
    private var saveActivityDialog: SaveActivityDialog? = null

    private var audioEnable = true
    private var countLastLap: Boolean = true
    private var lapDistance: Float = 50.0f
    private val sportProfile by lazy(LazyThreadSafetyMode.NONE) { SportProfileProvider.get() }

    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { updateParameters() }

    private fun <T : View> findBoundView(id: Int) = binding.root.findViewById<T>(id)
    private val toolbar by lazy(LazyThreadSafetyMode.NONE) { findBoundView<Toolbar>(R.id.toolbar) }
    private val bt_start by lazy(LazyThreadSafetyMode.NONE) { findBoundView<Button>(R.id.bt_start) }
    private val bt_resume by lazy(LazyThreadSafetyMode.NONE) { findBoundView<Button>(R.id.bt_resume) }
    private val bt_lap by lazy(LazyThreadSafetyMode.NONE) { findBoundView<Button>(R.id.bt_lap) }
    private val bt_pause by lazy(LazyThreadSafetyMode.NONE) { findBoundView<Button>(R.id.bt_pause) }
    private val bt_share by lazy(LazyThreadSafetyMode.NONE) { findBoundView<Button>(R.id.bt_share) }
    private val bt_save by lazy(LazyThreadSafetyMode.NONE) { findBoundView<Button>(R.id.bt_save) }
    private val bt_restart by lazy(LazyThreadSafetyMode.NONE) { findBoundView<Button>(R.id.bt_restart) }
    private val bt_stop by lazy(LazyThreadSafetyMode.NONE) { findBoundView<Button>(R.id.bt_stop) }
    private val buttons_frame_2 by lazy(LazyThreadSafetyMode.NONE) { findBoundView<FrameLayout>(R.id.buttons_frame_2) }
    private val buttons_lay_restart_save by lazy(LazyThreadSafetyMode.NONE) { findBoundView<LinearLayout>(R.id.buttons_lay_restart_save) }
    private val layout_chronometer_pause by lazy(LazyThreadSafetyMode.NONE) { findBoundView<LinearLayout>(R.id.layout_chronometer_pause) }
    private val chronometer_lap_log by lazy(LazyThreadSafetyMode.NONE) { findBoundView<LinearLayout>(R.id.chronometer_lap_log) }
    private val text_view_empty by lazy(LazyThreadSafetyMode.NONE) { findBoundView<TextView>(R.id.text_view_empty) }
    private val info_lap by lazy(LazyThreadSafetyMode.NONE) { findBoundView<TextView>(R.id.info_lap) }
    private val info_travelled by lazy(LazyThreadSafetyMode.NONE) { findBoundView<TextView>(R.id.info_travelled) }
    private val info_metric by lazy(LazyThreadSafetyMode.NONE) { findBoundView<TextView>(R.id.info_pace) }
    private val recycler_view by lazy(LazyThreadSafetyMode.NONE) { findBoundView<RecyclerView>(R.id.recycler_view) }
    private val numberOfLap by lazy(LazyThreadSafetyMode.NONE) { findBoundView<TextView>(R.id.numberOfLap) }
    private val chronometerWidget by lazy(LazyThreadSafetyMode.NONE) { findBoundView<ChronometerWidget>(R.id.chronometerWidget) }
    private val chronometerWidgetPause by lazy(LazyThreadSafetyMode.NONE) { findBoundView<ChronometerWidget>(R.id.chronometerWidgetPause) }
    private val chronometerLogCurrent by lazy(LazyThreadSafetyMode.NONE) { findBoundView<ChronometerWidget>(R.id.chronometerLogCurrent) }
    private val chronometerLogWidget by lazy(LazyThreadSafetyMode.NONE) { findBoundView<ChronometerWidget>(R.id.chronometerLogWidget) }
    private val chronometerLogPause by lazy(LazyThreadSafetyMode.NONE) { findBoundView<ChronometerWidget>(R.id.chronometerLogPause) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        AdsManager.requestConsent(this)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = showDialogExit()
        })
        setSupportActionBar(toolbar)
        configureOnClicks()
        viewModel = loadViewModel()
        if (null != savedInstanceState) {
            restoreInstanceVisibilityViews(savedInstanceState)
            onRestoreChronometer()
        }
        updateParameters()
    }

    private fun loadViewModel() =
        ViewModelProvider(this, InjectorUtils.provideActivityViewModelFactory())
            .get(ChronometerViewModel::class.java)

    private fun configureOnClicks() {
        bt_start.setOnClickListener { btStartPressed().run { alarm(R.raw.beep) } }
        bt_resume.setOnClickListener { btResumePressed().run { alarm(R.raw.beep) } }
        bt_lap.setOnClickListener { btLapPressed().run { alarm(R.raw.beep_2) } }
        bt_pause.setOnClickListener { btPausePressed().run { alarm(R.raw.beep) } }
        bt_share.setOnClickListener { btSharePressed() }
        bt_save.setOnClickListener { btSavePressed() }
        bt_restart.setOnClickListener { btRestartPressed() }
        bt_stop.setOnClickListener { btStopPressed() }
    }

    private fun onRestoreChronometer() {
        refreshBasesTotal()
        checkAndPerformChronometerStatus()
        refreshChronometerLog()
        createAdapter()
    }

    private fun checkAndPerformChronometerStatus() {
        when {
            viewModel.chronometer.isStarted() -> {
                startChronometersWidget()
                checkHavePauseTime()
            }
            viewModel.chronometer.isPaused() -> startChronometersPaused()
            viewModel.chronometer.isFinished() -> refreshBasesPaused()
        }
    }

    private fun checkHavePauseTime() {
        if ((SystemClock.elapsedRealtime() - viewModel.chronometer.getLastLapBasePause()) > 1) {
            chronometerLogPause.setTextColor(Color.RED)
            chronometerLogPause.base = viewModel.chronometer.getLastLapBasePause()
        }
    }

    private fun restoreInstanceVisibilityViews(savedInstanceState: Bundle) {
        buttons_frame_2.visibility = savedInstanceState.getInt(buttons_frame_2.id.toString())
        buttons_lay_restart_save.visibility =
            savedInstanceState.getInt(buttons_lay_restart_save.id.toString())
        bt_start.visibility = savedInstanceState.getInt(bt_start.id.toString())
        bt_lap.visibility = savedInstanceState.getInt(bt_lap.id.toString())
        bt_pause.visibility = savedInstanceState.getInt(bt_pause.id.toString())
        bt_resume.visibility = savedInstanceState.getInt(bt_resume.id.toString())
        bt_stop.visibility = savedInstanceState.getInt(bt_stop.id.toString())
        layout_chronometer_pause.visibility =
            savedInstanceState.getInt(layout_chronometer_pause.id.toString())
        chronometer_lap_log.visibility =
            savedInstanceState.getInt(chronometer_lap_log.id.toString())
        text_view_empty.visibility = savedInstanceState.getInt(text_view_empty.id.toString())
    }

    private fun updateParameters() {
        lapDistance = getLapDistance(this)
        countLastLap = countLastLap(this)
        audioEnable = isAudioEnable(this)
        updateInfoTravelled()
    }


    private fun updateInfoTravelled() {
        val chronometer = viewModel.chronometer.chronometer
        val distance = LapsCounterUtils.getDistanceTravelled(chronometer, lapDistance, countLastLap)
        val presentation = presentSportMetrics(
            runningTimeMillis = LapsCounterUtils.getRunningTime(chronometer, countLastLap),
            splitDistanceMeters = lapDistance,
            travelledDistanceMeters = distance,
            profile = sportProfile,
        )
        info_lap.text = getString(R.string.info_distance_lap, presentation.splitDistance)
        info_travelled.text = getString(R.string.info_travelled, presentation.travelledDistance)
        info_metric.text = getString(
            R.string.info_metric,
            sportMetricTitle(sportProfile),
            presentation.metric,
        )
    }


    private fun createAdapter() {
        recycler_view.adapter = LapsAdapter(this, viewModel.chronometer.chronometer.getLaps())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        saveInstanceVisibilityViews(outState)
        super.onSaveInstanceState(outState)
    }

    private fun saveInstanceVisibilityViews(outState: Bundle) {
        outState.putInt(buttons_frame_2.id.toString(), buttons_frame_2.visibility)
        outState.putInt(buttons_lay_restart_save.id.toString(), buttons_lay_restart_save.visibility)
        outState.putInt(bt_start.id.toString(), bt_start.visibility)
        outState.putInt(bt_stop.id.toString(), bt_stop.visibility)
        outState.putInt(bt_lap.id.toString(), bt_lap.visibility)
        outState.putInt(bt_pause.id.toString(), bt_pause.visibility)
        outState.putInt(bt_resume.id.toString(), bt_resume.visibility)
        outState.putInt(layout_chronometer_pause.id.toString(), layout_chronometer_pause.visibility)
        outState.putInt(chronometer_lap_log.id.toString(), chronometer_lap_log.visibility)
        outState.putInt(text_view_empty.id.toString(), text_view_empty.visibility)
    }

    private fun btStopPressed() {
        bt_stop.visibility = View.GONE
        bt_resume.visibility = View.GONE
        layout_chronometer_pause.visibility = View.GONE
        buttons_lay_restart_save.visibility = View.VISIBLE
        viewModel.chronometer.stop()
        chronometerWidgetPause.stop()
        chronometerLogPause.stop()
        refreshBasesPaused()
        updateInfoTravelled()
    }

    private fun btStartPressed() {
        createAdapter()
        btStartResumePressed()
        buttons_frame_2.visibility = View.VISIBLE
        bt_start.visibility = View.GONE
        bt_pause.visibility = View.VISIBLE
        chronometer_lap_log.visibility = View.VISIBLE
        text_view_empty.visibility = View.GONE
        refreshChronometerLog()
    }

    private fun btResumePressed() {
        btStartResumePressed()
        changeLayoutPauseResume(false)
        chronometerLogPause.stop()
        chronometerWidgetPause.stop()
    }

    private fun btStartResumePressed() {
        viewModel.chronometer.start()
        refreshBasesTotal()
        startChronometersWidget()
        refreshChronometerLog()
    }

    private fun btPausePressed() {
        viewModel.chronometer.pause()
        chronometerWidget.stop()
        chronometerLogWidget.stop()
        chronometerLogCurrent.stop()
        startChronometersPaused()
        changeLayoutPauseResume(true)
    }

    private fun btLapPressed() {
        viewModel.chronometer.lap()
        chronometerLogPause.base = SystemClock.elapsedRealtime()
        chronometerLogPause.setTextColor(ContextCompat.getColor(this, R.color.colorSecondaryText))
        refreshChronometerLog()
        recycler_view.smoothScrollToPosition()
        updateInfoTravelled()
    }

    private fun RecyclerView.smoothScrollToPosition() {
        adapter?.let {
            it.notifyDataSetChanged()
            smoothScrollToPosition(it.itemCount)
        }
    }

    private fun alarm(rawId: Int) {
        if (!audioEnable) return
        alarmAsync(this, rawId)
    }

    private fun refreshBasesTotal() {
        chronometerWidget.base = viewModel.chronometer.getCurrentBase()
        chronometerLogWidget.base = viewModel.chronometer.getCurrentBase()
    }

    private fun refreshBasesPaused() {
        chronometerWidgetPause.base = viewModel.chronometer.getPauseBaseTime()
        chronometerLogPause.base = viewModel.chronometer.getLastLapBasePause()
    }

    private fun startChronometersWidget() {
        chronometerWidget.start()
        chronometerLogWidget.start()
        chronometerLogCurrent.start()
    }

    private fun refreshChronometerLog() {
        numberOfLap.text =
            getString(
                R.string.num_lap,
                String.format("%02d", viewModel.chronometer.chronometer.getLaps().size)
            )
        chronometerLogCurrent.base = viewModel.chronometer.getBaseLastLap()
    }

    private fun startChronometersPaused() {
        refreshBasesPaused()
        chronometerLogPause.setTextColor(Color.RED)
        chronometerWidgetPause.start()
        chronometerLogPause.start()
    }

    private fun changeLayoutPauseResume(paused: Boolean) {
        buttons_lay_restart_save.visibility = View.GONE
        bt_stop.visibility = if (paused) View.VISIBLE else View.GONE
        bt_lap.visibility = if (paused) View.GONE else View.VISIBLE
        bt_pause.visibility = if (paused) View.GONE else View.VISIBLE
        bt_resume.visibility = if (paused) View.VISIBLE else View.GONE
        layout_chronometer_pause.visibility = if (paused) View.VISIBLE else View.INVISIBLE
    }

    private fun btSavePressed() {
        showDialogSaveActivity()
    }

    private fun showDialogSaveActivity() {
        SaveActivityDialog(this).also {
            saveActivityDialog = it
        }.show { name, description ->
            saveActivity(name, description)
        }
    }

    private fun saveActivity(name: String, description: String) {
        viewModel.saveActivity(name, description, lapDistance, countLastLap)
        showDialogActivitySaved()
    }

    private fun showDialogActivitySaved() {
        dialog = createDialogActivitySaved()
        configureDialogAnimation(dialog!!)
        dialog!!.show()
        runDismissDelayDialog(1500)
    }

    private fun runDismissDelayDialog(timeDelay: Long) {
        thread {
            Thread.sleep(timeDelay)
            dismissDialog()
        }
    }

    private fun createDialogActivitySaved(): AlertDialog? {
        return AlertDialog.Builder(this).apply {
            setView(R.layout.dialog_activity_saved)
        }.create()
    }

    private fun configureDialogAnimation(dialog: AlertDialog) {
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.DialogActivitySavedAnimation
    }

    private fun btSharePressed() {
        ChronometerShareUtils.shareText(
            this,
            viewModel.chronometer.chronometer,
            lapDistance,
            countLastLap
        )
    }

    private fun btRestartPressed() {
        resetChronometerWidgets()
        resetVisibleViews()
        updateInfoTravelled()
    }

    private fun resetChronometerWidgets() {
        viewModel.chronometer.reset()

        chronometerWidget.base = SystemClock.elapsedRealtime()
        chronometerLogWidget.base = SystemClock.elapsedRealtime()
        chronometerLogCurrent.base = SystemClock.elapsedRealtime()
        chronometerLogPause.base = SystemClock.elapsedRealtime()

        chronometerLogCurrent.stop()
        chronometerWidget.stop()
        chronometerLogWidget.stop()
        chronometerLogPause.stop()
    }

    private fun resetVisibleViews() {
        buttons_frame_2.visibility = View.GONE
        bt_start.visibility = View.VISIBLE
        buttons_lay_restart_save.visibility = View.GONE
        bt_lap.visibility = View.VISIBLE
        bt_pause.visibility = View.GONE
        bt_resume.visibility = View.GONE
        layout_chronometer_pause.visibility = View.INVISIBLE
        chronometer_lap_log.visibility = View.GONE
        text_view_empty.visibility = View.VISIBLE
        chronometerLogPause.setTextColor(ContextCompat.getColor(this, R.color.colorSecondaryText))
        recycler_view.adapter = null
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_settings -> settingsLauncher.launch(Intent(this, SettingsActivity::class.java))
            R.id.menu_activities -> startActivity(Intent(this, ActivitiesActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        configureHeadsetButtons()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (headsetButtonsEnable() && event.action == KeyEvent.ACTION_DOWN &&
            (keyCode == KeyEvent.KEYCODE_HEADSETHOOK ||
                keyCode == KeyEvent.KEYCODE_MEDIA_NEXT ||
                keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        ) {
            headsetButtonReceiver?.dispatchMediaButtonEvent(this, event)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun configureHeadsetButtons() {
        if (headsetButtonsEnable()) {
            if (null == headsetButtonReceiver) headsetButtonReceiver = HeadsetButtonControl()
            headsetButtonReceiver!!.registerHeadsetButton(this, this)
        }
    }

    private fun headsetButtonsEnable() = PreferenceManager.getDefaultSharedPreferences(this)
        .getBoolean(
            getString(R.string.pref_headset_buttons_name),
            resources.getBoolean(R.bool.pref_headset_buttons_default_value)
        )


    override fun onPause() {
        headsetButtonReceiver?.unregisterHeadsetButton(this)
        dismissDialog()
        saveActivityDialog?.dismiss()
        super.onPause()
    }

    private fun dismissDialog() {
        dialog?.let {
            it.dismiss()
            dialog = null
        }
    }

    override fun btHeadsetHookPressed(keyEvent: KeyEvent?) {
        when (View.VISIBLE) {
            bt_start.visibility -> btStartPressed()
            bt_resume.visibility -> btResumePressed()
            bt_lap.visibility -> btLapPressed()
        }
    }

    override fun btHeadsetHookDoubleClickPressed(keyEvent: KeyEvent?) {
        if (View.VISIBLE == bt_pause.visibility) btPausePressed()
    }

    override fun btHeadsetMediaNextPressed(keyEvent: KeyEvent?) {
    }

    override fun btHeadsetMediaPreviousPressed(keyEvent: KeyEvent?) {
    }

    override fun btHeadsetUndefinedPressed(keyEvent: KeyEvent?) {
    }

    private fun showDialogExit() {
        AlertDialog.Builder(this).apply {
            setTitle(R.string.app_name)
            setMessage(R.string.confirm_exit)
            setPositiveButton(android.R.string.ok) { _, _ -> super.finish() }
            setNegativeButton(android.R.string.cancel, null)
        }.let { dialog = it.create(); dialog!! }.let { it.show() }
    }

}
