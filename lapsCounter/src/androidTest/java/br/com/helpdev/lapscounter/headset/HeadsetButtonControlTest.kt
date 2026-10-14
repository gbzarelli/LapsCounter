package br.com.helpdev.lapscounter.headset

import android.view.KeyEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class HeadsetButtonControlTest {

    @Test
    fun headsetHookEventRoutesToListener() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val control = HeadsetButtonControl()
        val hookPressed = CountDownLatch(1)
        val listener = object : HeadsetButtonControl.HeadsetButtonControlListener {
            override fun btHeadsetHookPressed(keyEvent: KeyEvent?) {
                hookPressed.countDown()
            }

            override fun btHeadsetHookDoubleClickPressed(keyEvent: KeyEvent?) = Unit
            override fun btHeadsetMediaNextPressed(keyEvent: KeyEvent?) = Unit
            override fun btHeadsetMediaPreviousPressed(keyEvent: KeyEvent?) = Unit
            override fun btHeadsetUndefinedPressed(keyEvent: KeyEvent?) = Unit
        }

        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                control.registerHeadsetButton(context, listener)
            }
            control.dispatchMediaButtonEvent(
                context,
                KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK)
            )

            assertTrue("Headset hook should reach listener", hookPressed.await(2, TimeUnit.SECONDS))
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                control.unregisterHeadsetButton(context)
            }
        }
    }
}
