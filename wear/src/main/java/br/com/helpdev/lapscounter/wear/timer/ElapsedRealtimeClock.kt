package br.com.helpdev.lapscounter.wear.timer

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import java.util.UUID

// If BOOT_COUNT cannot be read, only this process can prove its anchors are valid.
// A new process gets a different identity and conservatively offers checkpoint recovery.
private val processClockIdentity = "process:${UUID.randomUUID()}"

fun interface ElapsedRealtimeClock {
    fun nowMillis(): Long
    fun bootIdentity(): String? = processClockIdentity
}

object AndroidElapsedRealtimeClock : ElapsedRealtimeClock {
    override fun nowMillis(): Long = SystemClock.elapsedRealtime()
}

internal fun readBootIdentity(readBootCount: () -> Int): String {
    val count = try { readBootCount().takeIf { it >= 0 } } catch (_: Exception) { null }
    return count?.let { "boot:$it" } ?: processClockIdentity
}

/** BOOT_COUNT is public since API 24; missing/restricted provider values fail safe. */
fun bootAwareElapsedRealtimeClock(context: Context): ElapsedRealtimeClock {
    val identity = readBootIdentity { Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1) }
    return object : ElapsedRealtimeClock {
        override fun nowMillis() = SystemClock.elapsedRealtime()
        override fun bootIdentity() = identity
    }
}
