package br.com.helpdev.lapscounter.sync

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.UUID

/** Version 1: big-endian primitives, UTF-8 strings prefixed with a signed 32-bit byte length. */
object ActivitySyncCodec {
    const val MAX_BYTES = 1024 * 1024
    const val MAX_LAPS = 10000
    private const val MAX_STRING = 65536
    private const val MAGIC = 0x4c435359

    fun encode(record: ActivitySyncRecord): ByteArray {
        validate(record)
        val buffer = ByteArrayOutputStream()
        DataOutputStream(buffer).use { out ->
            out.writeInt(MAGIC); out.writeInt(record.schemaVersion)
            out.string(record.sportId); out.string(record.activityId); out.string(record.name)
            out.writeBoolean(record.description != null); record.description?.let { out.string(it) }
            out.writeLong(record.startTimestampMillis); out.writeFloat(record.lapDistanceMeters)
            out.writeFloat(record.travelledDistanceMeters); out.writeBoolean(record.countLastLap)
            out.writeLong(record.runningTimeMillis); out.writeLong(record.pausedTimeMillis)
            out.writeInt(record.laps.size)
            record.laps.forEach { out.writeInt(it.index); out.writeLong(it.durationMillis); out.writeLong(it.elapsedMillis); out.writeBoolean(it.isFinalPartialLap) }
        }
        return buffer.toByteArray().also { require(it.size <= MAX_BYTES) }
    }

    fun decode(bytes: ByteArray): ActivitySyncRecord {
        require(bytes.size <= MAX_BYTES)
        try {
            val input = DataInputStream(ByteArrayInputStream(bytes))
            require(input.readInt() == MAGIC)
            val version = input.readInt(); require(version == ActivitySyncRecord.CURRENT_VERSION)
            val sport = input.string(); val id = input.string(); val name = input.string()
            val description = if (input.bool()) input.string() else null
            val start = input.readLong(); val distance = input.readFloat(); val travelled = input.readFloat()
            val countLast = input.bool(); val running = input.readLong(); val paused = input.readLong()
            val count = input.readInt(); require(count in 0..MAX_LAPS && count.toLong() * 21 <= input.available())
            val laps = List(count) { ActivitySyncLap(input.readInt(), input.readLong(), input.readLong(), input.bool()) }
            require(input.available() == 0)
            return ActivitySyncRecord(version, sport, id, name, description, start, distance, travelled, countLast, running, paused, laps).also(::validate)
        } catch (e: IllegalArgumentException) { throw e } catch (e: Exception) { throw IllegalArgumentException("Invalid activity payload", e) }
    }

    private fun validate(r: ActivitySyncRecord) {
        require(r.schemaVersion == ActivitySyncRecord.CURRENT_VERSION)
        require(r.sportId in setOf("swimming", "running", "cycling"))
        require(UUID.fromString(r.activityId).toString() == r.activityId)
        require(r.name.isNotBlank() && r.startTimestampMillis >= 0)
        require(r.lapDistanceMeters.isFinite() && r.lapDistanceMeters > 0 && r.travelledDistanceMeters.isFinite() && r.travelledDistanceMeters >= 0)
        require(r.runningTimeMillis >= 0 && r.pausedTimeMillis >= 0 && r.laps.size <= MAX_LAPS)
        require(r.laps.all { it.index > 0 && it.durationMillis >= 0 && it.elapsedMillis >= 0 } && r.laps.map { it.index }.toSet().size == r.laps.size)
        listOfNotNull(r.sportId, r.activityId, r.name, r.description).forEach { require(it.toByteArray(Charsets.UTF_8).size <= MAX_STRING) }
    }
    private fun DataOutputStream.string(value: String) { val b = value.toByteArray(Charsets.UTF_8); writeInt(b.size); write(b) }
    private fun DataInputStream.string(): String {
        val size = readInt(); require(size in 0..MAX_STRING && size <= available()); val b = ByteArray(size); readFully(b)
        return Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(b)).toString()
    }
    private fun DataInputStream.bool(): Boolean = when (readUnsignedByte()) { 0 -> false; 1 -> true; else -> throw IllegalArgumentException("Invalid boolean") }
}
