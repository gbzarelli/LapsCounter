package br.com.helpdev.lapscounter.headset.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import java.util.concurrent.atomic.AtomicInteger

class HeadsetButtonReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val intentAction = intent.action
        if (Intent.ACTION_MEDIA_BUTTON != intentAction) {
            return
        }
        val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return
        val action = event.action

        if (action == KeyEvent.ACTION_DOWN) {
            val keycode = event.keyCode
            val broadcast = when (keycode) {
                KeyEvent.KEYCODE_MEDIA_NEXT -> Intent(ACTION_KEYCODE_MEDIA_NEXT)
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> Intent(ACTION_KEYCODE_MEDIA_PREVIOUS)
                KeyEvent.KEYCODE_HEADSETHOOK -> {
                    if (1 == clickHook.addAndGet(1)) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            sendBroadcast(context, intent, event,
                                    Intent(if (1 == clickHook.get()) ACTION_KEYCODE_HEADSET_HOOK
                                    else ACTION_KEYCODE_HEADSET_HOOK_DOUBLE_CLICK))
                            clickHook.set(0)
                        }, 400)
                    }
                    return
                }
                else -> Intent(ACTION_KEYCODE_UNDEFINED)
            }
            sendBroadcast(context, intent, event, broadcast)
        }
    }

    private fun sendBroadcast(context: Context, intent: Intent, event: KeyEvent, broadcast: Intent) {
        broadcast.putExtra(PARAM_KEY_EVENT, event)
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(context).sendBroadcast(broadcast)
    }

    companion object {
        val clickHook = AtomicInteger(0)
        const val ACTION_KEYCODE_MEDIA_NEXT = "ACTION_KEYCODE_MEDIA_NEXT"
        const val ACTION_KEYCODE_MEDIA_PREVIOUS = "ACTION_KEYCODE_MEDIA_PREVIOUS"
        const val ACTION_KEYCODE_HEADSET_HOOK = "ACTION_KEYCODE_HEADSET_HOOK"
        const val ACTION_KEYCODE_HEADSET_HOOK_DOUBLE_CLICK = "ACTION_KEYCODE_HEADSET_HOOK_DOUBLE_CLICK"
        const val ACTION_KEYCODE_UNDEFINED = "ACTION_KEYCODE_UNDEFINED"
        const val PARAM_KEY_EVENT = "PARAM_KEY_EVENT"
    }

}
