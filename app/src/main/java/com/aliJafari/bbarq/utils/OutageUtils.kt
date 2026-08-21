package com.aliJafari.bbarq.utils

import com.aliJafari.bbarq.data.model.Outage
import saman.zamani.persiandate.PersianDate
import java.util.Calendar

fun String.toEpochMillis(time: String): Long {
    return try {
        val dParts = split('/')
        if (dParts.size != 3) return -1
        val safeTime = time.takeIf { it.contains(':') } ?: "00:00"
        val tParts = safeTime.split(':')
        val pDate = PersianDate().also {
            it.shYear = dParts[0].toInt()
            it.shMonth = dParts[1].toInt()
            it.shDay = dParts[2].toInt()
            it.hour = tParts[0].toIntOrNull() ?: 0
            it.minute = tParts.getOrNull(1)?.toIntOrNull() ?: 0
        }
        Calendar.getInstance().apply {
            set(pDate.grgYear, pDate.grgMonth - 1, pDate.grgDay, pDate.hour, pDate.minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    } catch (e: Exception) {
        -1
    }
}

fun Outage.toEpochMillis(): Long = date?.toEpochMillis(startTime ?: "00:00") ?: -1

private fun normalizeTime(time: String): String {
    val parts = time.split(':')
    return "${parts[0]}:${parts[1]}"
}

private fun addTwoHours(time: String): String {
    val (h, m) = time.split(':').map { it.toInt() }
    val totalMinutes = (h * 60 + m + 120) % (24 * 60)
    return "%02d:%02d".format(totalMinutes / 60, totalMinutes % 60)
}

fun correctOutageTimes(outage: Outage): Outage {
    val start = outage.startTime
    val end = outage.endTime
    if (!start.isNullOrBlank() || end.isNullOrBlank()) return outage.copy(
        startTime = start?.let { normalizeTime(it) },
        endTime = end?.let { normalizeTime(it) }
    )
    val correctedStart = normalizeTime(end)
    return outage.copy(startTime = correctedStart, endTime = addTwoHours(correctedStart))
}