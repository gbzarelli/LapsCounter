package br.com.helpdev.lapscounter.wear.timer

import android.os.SystemClock

fun interface ElapsedRealtimeClock {
    fun nowMillis(): Long
}

object AndroidElapsedRealtimeClock : ElapsedRealtimeClock {
    override fun nowMillis(): Long = SystemClock.elapsedRealtime()
}
