package com.aliJafari.bbarq.utils

import com.aliJafari.bbarq.data.model.Outage
import saman.zamani.persiandate.PersianDate
import java.util.Calendar

fun Outage.toEpochMillis(): Long {
    val d = date ?: return Long.MAX_VALUE
    val t = startTime ?: "00:00"
    val pDate = PersianDate().also {
        it.shYear = d.split('/')[0].toInt()
        it.shMonth = d.split('/')[1].toInt()
        it.shDay = d.split('/')[2].toInt()
        it.hour = t.split(':')[0].toInt()
        it.minute = t.split(':')[1].toInt()
    }
    return Calendar.getInstance().apply {
        set(pDate.grgYear, pDate.grgMonth - 1, pDate.grgDay, pDate.hour, pDate.minute, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
