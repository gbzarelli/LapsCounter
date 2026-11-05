package br.com.helpdev.lapscounter.wear.sync

import br.com.helpdev.lapscounter.sync.ActivitySyncRecord

fun interface ActivitySyncTransport { suspend fun send(record: ActivitySyncRecord) }
