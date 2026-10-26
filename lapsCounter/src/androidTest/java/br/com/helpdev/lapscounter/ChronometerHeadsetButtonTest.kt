package br.com.helpdev.lapscounter

import android.app.Activity
import android.content.Intent
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.helpdev.lapscounter.headset.receiver.HeadsetButtonReceiver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChronometerHeadsetButtonTest {

    @Test
    fun headsetButtonStartsMarksLapPausesAndResumesChronometer() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val preferenceKey = context.getString(R.string.pref_headset_buttons_name)
        val hadPreviousValue = preferences.contains(preferenceKey)
        val previousValue = preferences.getBoolean(
            preferenceKey,
            context.resources.getBoolean(R.bool.pref_headset_buttons_default_value)
        )
        preferences.edit().putBoolean(preferenceKey, true).commit()

        val intent = Intent(context, ChronometerActivityImpl::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        val activity = instrumentation.startActivitySync(intent) as ChronometerActivity

        try {
            assertTrue(
                "Headset control preference should be enabled",
                PreferenceManager.getDefaultSharedPreferences(activity)
                    .getBoolean(preferenceKey, false)
            )

            dispatchHeadsetClick(instrumentation)
            awaitCondition(instrumentation, activity, "single click should start the chronometer") {
                activity.button(R.id.bt_start).visibility == View.GONE &&
                    activity.button(R.id.bt_pause).visibility == View.VISIBLE &&
                    activity.button(R.id.bt_lap).visibility == View.VISIBLE
            }

            dispatchHeadsetClick(instrumentation)
            awaitCondition(instrumentation, activity, "single click should mark a lap") {
                activity.findViewById<TextView>(R.id.numberOfLap).text.toString().endsWith("01")
            }

            dispatchDoubleHeadsetClick(instrumentation)
            awaitCondition(instrumentation, activity, "double click should pause the chronometer") {
                activity.button(R.id.bt_resume).visibility == View.VISIBLE &&
                    activity.button(R.id.bt_lap).visibility == View.GONE
            }
            assertEquals("Headset click counter should reset after double click", 0, HeadsetButtonReceiver.clickHook.get())

            dispatchHeadsetClick(instrumentation)
            awaitCondition(instrumentation, activity, "single click should resume the chronometer") {
                activity.button(R.id.bt_pause).visibility == View.VISIBLE &&
                    activity.button(R.id.bt_lap).visibility == View.VISIBLE
            }
        } finally {
            instrumentation.runOnMainSync { activity.finish() }
            if (hadPreviousValue) {
                preferences.edit().putBoolean(preferenceKey, previousValue).commit()
            } else {
                preferences.edit().remove(preferenceKey).commit()
            }
        }
    }

    private fun dispatchHeadsetClick(
        instrumentation: android.app.Instrumentation,
    ) {
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_HEADSETHOOK)
    }

    private fun dispatchDoubleHeadsetClick(
        instrumentation: android.app.Instrumentation,
    ) {
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_HEADSETHOOK)
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_HEADSETHOOK)
    }

    private fun awaitCondition(
        instrumentation: android.app.Instrumentation,
        activity: Activity,
        message: String,
        condition: () -> Boolean,
    ) {
        val deadline = SystemClock.uptimeMillis() + 2_000
        var satisfied = false
        while (SystemClock.uptimeMillis() < deadline && !satisfied) {
            instrumentation.runOnMainSync { satisfied = condition() }
            if (!satisfied) Thread.sleep(25)
        }
        var state = ""
        instrumentation.runOnMainSync {
            state = "start=${activity.button(R.id.bt_start).visibility}, " +
                "pause=${activity.button(R.id.bt_pause).visibility}, " +
                "resume=${activity.button(R.id.bt_resume).visibility}, " +
                "lap=${activity.button(R.id.bt_lap).visibility}, " +
                "lapCount=${activity.findViewById<TextView>(R.id.numberOfLap).text}"
        }
        assertTrue("$message ($state, pendingClicks=${HeadsetButtonReceiver.clickHook.get()})", satisfied)
    }

    private fun Activity.button(id: Int) = findViewById<Button>(id)
}
