package com.aliJafari.bbarq.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aliJafari.bbarq.R
import com.aliJafari.bbarq.data.model.Outage
import saman.zamani.persiandate.PersianDate
import java.util.Calendar
import java.util.TimeZone

class ReminderReceiver : BroadcastReceiver() {
    val channelId = "outage_reminder"
    override fun onReceive(context: Context, intent: Intent) {
        Log.e("TAG", "onReceive: received shit")
        val placeName = intent.getStringExtra("placeName")
        val startTime = intent.getStringExtra("startTime")
        val endTime = intent.getStringExtra("endTime")

        val titles = context.resources.getStringArray(R.array.power_reminder_titles)
        val randomTitle = titles.random()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, context.getString(R.string.notification_channel_blackout_checker), NotificationManager.IMPORTANCE_LOW
            )
            context.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
        notificationManager.notify(
            intent.hashCode(),
            NotificationCompat.Builder(context, channelId).setContentTitle(randomTitle)
                .setContentText(
                    context.getString(
                        R.string.reminder_notification_text,
                        placeName,
                        startTime,
                        endTime
                    )
                ).setSilent(false)
                .setGroup("outage")
                .setSmallIcon(R.drawable.electricity_caution_svgrepo_com).build()
        )
    }
}

fun scheduleReminder(context: Context, outage: Outage, placeName: String, offset: ReminderOffset, enabled: Boolean) {

    fun getTimestampFromPersianDate(pDate: PersianDate): Long {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.set(pDate.grgYear, pDate.grgMonth - 1, pDate.grgDay, pDate.hour, pDate.minute, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val requestCode = "${outage.id}-${outage.date}-${outage.startTime}-${offset.bit}".hashCode()

    val intent = Intent(context, ReminderReceiver::class.java).also {
        it.putExtra("placeName", placeName)
        it.putExtra("startTime", outage.startTime)
        it.putExtra("endTime", outage.endTime)
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context, requestCode, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    alarmManager.cancel(pendingIntent)
    if (!enabled) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) return

    val pDate = PersianDate().also {
        it.shYear = outage.date!!.split('/')[0].toInt()
        it.shMonth = outage.date.split('/')[1].toInt()
        it.shDay = outage.date.split('/')[2].toInt()
        it.hour = outage.startTime!!.split(':')[0].toInt()
        it.minute = outage.startTime.split(':')[1].toInt()
    }.subMinutes(offset.minutes)

    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, getTimestampFromPersianDate(pDate), pendingIntent)
}
