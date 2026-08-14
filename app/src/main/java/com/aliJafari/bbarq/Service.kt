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
import com.aliJafari.bbarq.data.repository.OutageRepository
import com.aliJafari.bbarq.data.repository.PlaceOutage
import com.aliJafari.bbarq.data.repository.PlaceRepository
import com.aliJafari.bbarq.ui.main.MainActivity
import com.aliJafari.bbarq.ui.main.ScheduleUrgency
import com.aliJafari.bbarq.ui.main.relativeStatus
import com.aliJafari.bbarq.utils.BillIDNot13Chars
import com.aliJafari.bbarq.utils.BillIDNotFoundException
import com.aliJafari.bbarq.utils.ReminderOffset
import com.aliJafari.bbarq.utils.RequestUnsuccessful
import com.aliJafari.bbarq.utils.toEpochMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ForegroundService : Service() {

    val handler = Handler(Looper.getMainLooper())
    lateinit var repository : OutageRepository
    lateinit var placeRepository: PlaceRepository
    private lateinit var notificationManager: NotificationManager
    private lateinit var prefs: SharedPreferences
    val channelId = "blackout_checker_channel"

    private var outagesCache: List<PlaceOutage> = emptyList()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action=="refresh"){
            fetchApiData()
            return START_STICKY
        }
        repository = OutageRepository(applicationContext)
        placeRepository = PlaceRepository(applicationContext)
        notificationManager = getSystemService(android.app.NotificationManager::class.java)
        prefs = applicationContext.getSharedPreferences("my_prefs", MODE_PRIVATE)
        startForeground(1, createNotification())
        startRepeatingTask()


        return START_STICKY
    }

    private fun fetchApiData() {
        CoroutineScope(Dispatchers.IO).launch {
            val schedules = mutableListOf<PlaceOutage>()
            val errors = mutableListOf<String>()
            val places = placeRepository.getPlaces()
            places.forEach { place ->
                try {
                    repository.fetchOutages(place.billId).forEach { outage ->
                        schedules.add(PlaceOutage(place = place, outage = outage))
                    }
                } catch (error: BillIDNotFoundException) {
                    errors.add(getString(R.string.place_fetch_invalid_bill_id, place.name))
                } catch (error: BillIDNot13Chars) {
                    errors.add(getString(R.string.place_fetch_invalid_count, place.name))
                } catch (error: RequestUnsuccessful) {
                    errors.add(getString(R.string.place_fetch_failed, place.name, error.details))
                }
            }
            val note = errors.joinToString("\n").let { if (it.isBlank()) "" else "$it\n" }
            scheduleReminder(schedules)
            updateNotification(if (schedules.isEmpty()) outagesCache else schedules, note)
            if (schedules.isNotEmpty()) outagesCache = schedules
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

    private fun scheduleReminder(outages: List<PlaceOutage>) {
        outages.forEach { po ->
            ReminderOffset.entries.forEach { offset ->
                val enabled = po.place.reminderOffsetsMask and offset.bit != 0
                com.aliJafari.bbarq.utils.scheduleReminder(applicationContext, po.outage, po.place.name, offset, enabled)
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

    fun updateNotification(schedules: List<PlaceOutage>, errors: String = "") {
        val active = schedules
            .map { it to relativeStatus(it.outage, applicationContext) }
            .filter { (_, status) -> status.urgency != ScheduleUrgency.ENDED }
            .sortedBy { (it, _) -> it.outage.toEpochMillis() }

        val lines = active.map { (po, status) ->
            "${po.place.name}: ${status.label} (${po.outage.startTime}-${po.outage.endTime})"
        }

        val summary = if (active.isEmpty() && errors.isBlank())
            resources.getStringArray(R.array.no_power_cut_messages).random()
        else if (errors.isNotBlank()) getString(R.string.network_request_failed)
        else active[0].second.label

        val style = NotificationCompat.InboxStyle()
        lines.forEach { style.addLine(it) }
        if (errors.isNotBlank()) errors.lines().forEach { it -> style.addLine(it) }

        notificationManager.notify(
            1,
            NotificationCompat.Builder(this, channelId)
                .setContentTitle(getString(R.string.notification_monitoring_title))
                .setContentText(summary)
                .setContentIntent(
                    PendingIntent.getActivity(this,6565,Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE)
                )
                .setStyle(style)
                .setSilent(true)
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
