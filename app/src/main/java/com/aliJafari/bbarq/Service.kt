package com.aliJafari.bbarq

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.aliJafari.bbarq.data.model.Outage
import com.aliJafari.bbarq.data.repository.OutageRepository
import com.aliJafari.bbarq.utils.BillIDNot13Chars
import com.aliJafari.bbarq.utils.BillIDNotFoundException
import com.aliJafari.bbarq.utils.RequestUnsuccessful
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ForegroundService : Service() {

    private var billId: String = ""
    val handler = Handler(Looper.getMainLooper())
    lateinit var repository : OutageRepository
    private lateinit var notificationManager: NotificationManager
    private lateinit var prefs: SharedPreferences
    val channelId = "blackout_checker_channel"

    private var outagesCache: List<Outage> = emptyList()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action=="refresh"){
            fetchApiData()
            return START_STICKY
        }
        repository = OutageRepository(applicationContext)
        notificationManager = getSystemService(android.app.NotificationManager::class.java)
        prefs = applicationContext.getSharedPreferences("my_prefs", MODE_PRIVATE)
        billId = prefs.getString("billId", "").toString()
        startForeground(1, createNotification())
        startRepeatingTask()


        return START_STICKY
    }

    private fun fetchApiData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.sendRequest(billId) {
                    scheduleReminder(it)
                    updateNotification(it)
                    outagesCache = it
                }
            } catch (error: BillIDNotFoundException) {
                updateNotification(outagesCache, getString(R.string.notification_update_error_unknown_bill_id))
            } catch (error: BillIDNot13Chars) {
                updateNotification(outagesCache, getString(R.string.notification_update_error_unknown_bill_id))
            } catch (error: RequestUnsuccessful) {
                updateNotification(outagesCache, getString(R.string.notification_update_error_connection_problem))
            }
        }
    }
    private fun startRepeatingTask() {
        val runnable = object : Runnable {
            override fun run() {
                fetchApiData()
                handler.postDelayed(this, 3600_000) // 1 hour delay
            }

        }
        handler.post(runnable)
    }

    private fun scheduleReminder(outages: List<Outage>) {
        outages.forEach {
            if (prefs.getBoolean("reminder", false)) {
                com.aliJafari.bbarq.utils.scheduleReminder(
                    applicationContext, it
                )
            }
        }
    }


    lateinit var refreshIntent : PendingIntent

    private fun createNotification(): Notification {
        refreshIntent = PendingIntent.getService(
            applicationContext, 555, Intent(applicationContext, ForegroundService::class.java).also {
                it.action = "refresh"
            }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, getString(R.string.notification_channel_blackout_checker), NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId).setContentTitle(getString(R.string.notification_monitoring_title))
            .setContentText(getString(R.string.notification_service_running)).setSilent(true)
            .addAction(R.drawable.ic_renew, getString(R.string.refresh), refreshIntent)
            .setSmallIcon(R.drawable.electricity_caution_svgrepo_com).build()
    }

    fun updateNotification(schedules: List<Outage>, note: String = "") {
        var content = note
        if (schedules.isEmpty()){

            val messages = resources.getStringArray(R.array.no_power_cut_messages)
            content += messages.random()
        }
        for (schedule in schedules) {
            content += getString(
                R.string.notification_schedule_line,
                schedule.date,
                schedule.startTime,
                schedule.endTime
            )
        }
        notificationManager.notify(
            1,
            NotificationCompat.Builder(this, channelId).setContentTitle(getString(R.string.notification_monitoring_title))
                .setContentText(content).setSilent(true)
                .addAction(R.drawable.ic_renew, getString(R.string.refresh), refreshIntent)
                .setSmallIcon(R.drawable.electricity_caution_svgrepo_com).build()
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    for (service in activityManager.getRunningServices(Int.MAX_VALUE)) {
        if (serviceClass.name == service.service.className && service.foreground) {
            return true
        }
    }
    return false
}
