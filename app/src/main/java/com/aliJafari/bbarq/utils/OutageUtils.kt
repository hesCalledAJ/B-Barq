package com.aliJafari.bbarq.utils

import com.aliJafari.bbarq.data.model.Outage
import saman.zamani.persiandate.PersianDate
import java.util.Calendar
import java.util.Locale

fun String.toEpochMillis(time: String): Long {
    return try {
        val dParts = split('/', '-')
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

fun normalizeTime(time: String): String {
    val parts = time.split(':')
    return "${parts[0]}:${parts[1]}"
}

fun fixTimeEdgeCases(outage: Outage): Outage {
    val startTime = if (outage.startTime.isNullOrBlank()) outage.time else outage.startTime
    return outage.copy(startTime = startTime?.let { normalizeTime(it)}, endTime = outage.endTime?.let { normalizeTime(it)} )
}