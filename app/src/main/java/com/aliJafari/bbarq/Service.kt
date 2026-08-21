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
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aliJafari.bbarq.data.model.Outage
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
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.collections.forEach

class ForegroundService : Service() {

    val handler = Handler(Looper.getMainLooper())
    lateinit var repository: OutageRepository
    lateinit var placeRepository: PlaceRepository
    private lateinit var notificationManager: NotificationManager
    private lateinit var prefs: SharedPreferences
    private lateinit var refreshIntent: PendingIntent
    private var initialized = false
    val channelId = "blackout_checker_channel"

    private val placeCache = mutableMapOf<Int, List<Outage>>()
    private val refreshHours = listOf(7, 12, 17, 21)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureInitialized()
        startForeground(1, createNotification())

        if (intent?.action == "refresh") {
            fetchApiData()
            return START_STICKY
        }

        scheduleNextNetworkRefresh()
        startNotificationTicker()
        fetchApiData()
        return START_STICKY
    }

    private fun ensureInitialized() {
        if (initialized) return
        repository = OutageRepository(applicationContext)
        placeRepository = PlaceRepository(applicationContext)
        notificationManager = getSystemService(NotificationManager::class.java)
        prefs = applicationContext.getSharedPreferences("my_prefs", MODE_PRIVATE)
        refreshIntent = PendingIntent.getService(
            applicationContext, 555,
            Intent(applicationContext, ForegroundService::class.java).apply { action = "refresh" },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, getString(R.string.notification_channel_blackout_checker), NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
        initialized = true
    }

    private fun fetchApiData() {
        CoroutineScope(Dispatchers.IO).launch {
            val errors = mutableListOf<String>()
            val places = placeRepository.getPlaces()

            places.forEach { place ->
                try {
                    placeCache[place.id.toInt()] = repository.fetchOutages(place.billId)
                } catch (e: BillIDNotFoundException) {
                    errors.add(getString(R.string.place_fetch_invalid_bill_id, place.name))
                } catch (e: BillIDNot13Chars) {
                    errors.add(getString(R.string.place_fetch_invalid_count, place.name))
                } catch (e: RequestUnsuccessful) {
                    errors.add(getString(R.string.place_fetch_failed, place.name, e.details))
                }
                // failure leaves placeCache[place.id] untouched -> stale but not lost
            }

            val schedules = places.flatMap { p -> placeCache[p.id.toInt()]?.map { PlaceOutage(p, it) } ?: emptyList() }
            val note = errors.joinToString("\n").let { if (it.isBlank()) "" else "$it\n" }
            scheduleReminder(schedules)
            updateNotification(schedules, note)
        }
    }

    private fun scheduleNextNetworkRefresh() {
        val now = Calendar.getInstance()
        val next = refreshHours
            .map { h -> Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) } }
            .firstOrNull { it.after(now) }
            ?: Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, refreshHours.first()); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
            }

        handler.postDelayed({ fetchApiData(); scheduleNextNetworkRefresh() }, next.timeInMillis - now.timeInMillis)
    }

    private fun startNotificationTicker() {
        lateinit var runnable: Runnable
        runnable = Runnable {
            CoroutineScope(Dispatchers.IO).launch {
                val places = placeRepository.getPlaces()
                val schedules = places.flatMap { p -> placeCache[p.id.toInt()]?.map { PlaceOutage(p, it) } ?: emptyList() }
                withContext(Dispatchers.Main) {
                    if (schedules.isNotEmpty()) updateNotification(schedules)
                    handler.postDelayed(runnable, tickerInterval(schedules))
                }
            }
        }
        handler.post(runnable)
    }

    private fun tickerInterval(schedules: List<PlaceOutage>): Long {
        val urgencies = schedules.map { relativeStatus(it.outage, applicationContext).urgency }
        return when {
            urgencies.any { it == ScheduleUrgency.ONGOING || it == ScheduleUrgency.SOON } -> 60_000L
            urgencies.any { it == ScheduleUrgency.TODAY } -> 5 * 60_000L
            else -> 15 * 60_000L
        }
    }

    private fun scheduleReminder(outages: List<PlaceOutage>) {
        outages.forEach { po ->
            ReminderOffset.entries.forEach { offset ->
                val enabled = po.place.reminderOffsetsMask and offset.bit != 0
                com.aliJafari.bbarq.utils.scheduleReminder(applicationContext, po.outage, po.place.name, offset, enabled)
            }
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.notification_monitoring_title))
            .setContentText(getString(R.string.notification_service_running))
            .setSilent(true)
            .addAction(R.drawable.ic_renew, getString(R.string.refresh), refreshIntent)
            .setSmallIcon(R.drawable.electricity_caution_svgrepo_com)
            .build()
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
        else if (active.isEmpty() && errors.isNotBlank()) getString(R.string.network_request_failed)
        else active[0].second.label

        val style = NotificationCompat.InboxStyle()
        lines.forEach { style.addLine(it) }
        if (errors.isNotBlank()) errors.lines().forEach { style.addLine(it) }

        notificationManager.notify(
            1,
            NotificationCompat.Builder(this, channelId)
                .setContentTitle(getString(R.string.notification_monitoring_title))
                .setContentText(summary)
                .setContentIntent(
                    PendingIntent.getActivity(this, 6565, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
                )
                .setStyle(style)
                .setSilent(true)
                .addAction(R.drawable.ic_renew, getString(R.string.refresh), refreshIntent)
                .setSmallIcon(R.drawable.electricity_caution_svgrepo_com)
                .build()
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
