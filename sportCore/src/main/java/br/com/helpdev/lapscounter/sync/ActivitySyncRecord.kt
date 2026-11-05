package br.com.helpdev.lapscounter.sync

data class ActivitySyncLap(val index: Int, val durationMillis: Long, val elapsedMillis: Long, val isFinalPartialLap: Boolean)

data class ActivitySyncRecord(
    val schemaVersion: Int, val sportId: String, val activityId: String, val name: String,
    val description: String?, val startTimestampMillis: Long, val lapDistanceMeters: Float,
    val travelledDistanceMeters: Float, val countLastLap: Boolean, val runningTimeMillis: Long,
    val pausedTimeMillis: Long, val laps: List<ActivitySyncLap>,
) {
    companion object { const val CURRENT_VERSION = 1 }
}
