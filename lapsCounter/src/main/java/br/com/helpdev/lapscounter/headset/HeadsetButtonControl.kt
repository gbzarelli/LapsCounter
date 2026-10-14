package br.com.helpdev.lapscounter.headset

import android.content.*
import android.view.KeyEvent
import br.com.helpdev.lapscounter.headset.receiver.HeadsetButtonReceiver

class HeadsetButtonControl {

    private var broadcastButtonReceiver: BroadcastButtonReceiver? = null
    private var headsetButtonControlListener: HeadsetButtonControlListener? = null

    fun registerHeadsetButton(context: Context, headsetButtonControlListener: HeadsetButtonControlListener) {
        unregisterHeadsetButton(context)

        this.headsetButtonControlListener = headsetButtonControlListener
        broadcastButtonReceiver = BroadcastButtonReceiver()
        val filter = IntentFilter()
        filter.addAction(HeadsetButtonReceiver.ACTION_KEYCODE_HEADSET_HOOK)
        filter.addAction(HeadsetButtonReceiver.ACTION_KEYCODE_MEDIA_NEXT)
        filter.addAction(HeadsetButtonReceiver.ACTION_KEYCODE_MEDIA_PREVIOUS)
        filter.addAction(HeadsetButtonReceiver.ACTION_KEYCODE_HEADSET_HOOK_DOUBLE_CLICK)
        filter.addAction(HeadsetButtonReceiver.ACTION_KEYCODE_UNDEFINED)
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(context)
            .registerReceiver(broadcastButtonReceiver!!, filter)
    }

    internal fun dispatchMediaButtonEvent(context: Context, event: KeyEvent) {
        val intent = Intent(Intent.ACTION_MEDIA_BUTTON).putExtra(Intent.EXTRA_KEY_EVENT, event)
        HeadsetButtonReceiver().onReceive(context, intent)
    }

    fun unregisterHeadsetButton(context: Context) {
        if (null != broadcastButtonReceiver) androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(
            context
        ).unregisterReceiver(broadcastButtonReceiver!!)
        broadcastButtonReceiver = null
        headsetButtonControlListener = null
    }

    interface HeadsetButtonControlListener {
        fun btHeadsetHookPressed(keyEvent: KeyEvent?)
        fun btHeadsetHookDoubleClickPressed(keyEvent: KeyEvent?)
        fun btHeadsetMediaNextPressed(keyEvent: KeyEvent?)
        fun btHeadsetMediaPreviousPressed(keyEvent: KeyEvent?)
        fun btHeadsetUndefinedPressed(keyEvent: KeyEvent?)
    }

    inner class BroadcastButtonReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (null == headsetButtonControlListener || null == intent) return
            val keyEvent = intent.getParcelableExtra<KeyEvent>(HeadsetButtonReceiver.PARAM_KEY_EVENT)
            when (intent.action) {
                HeadsetButtonReceiver.ACTION_KEYCODE_HEADSET_HOOK -> headsetButtonControlListener!!.btHeadsetHookPressed(keyEvent)
                HeadsetButtonReceiver.ACTION_KEYCODE_MEDIA_NEXT -> headsetButtonControlListener!!.btHeadsetMediaNextPressed(keyEvent)
                HeadsetButtonReceiver.ACTION_KEYCODE_MEDIA_PREVIOUS -> headsetButtonControlListener!!.btHeadsetMediaPreviousPressed(keyEvent)
                HeadsetButtonReceiver.ACTION_KEYCODE_UNDEFINED -> headsetButtonControlListener!!.btHeadsetUndefinedPressed(keyEvent)
                HeadsetButtonReceiver.ACTION_KEYCODE_HEADSET_HOOK_DOUBLE_CLICK -> headsetButtonControlListener!!.btHeadsetHookDoubleClickPressed(keyEvent)
            }
        }
    }

}
