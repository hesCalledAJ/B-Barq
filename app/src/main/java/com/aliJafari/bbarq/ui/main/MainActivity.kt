package com.aliJafari.bbarq.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.aliJafari.bbarq.ForegroundService
import com.aliJafari.bbarq.R
import com.aliJafari.bbarq.data.local.AuthStorage
import com.aliJafari.bbarq.data.model.Outage
import com.aliJafari.bbarq.data.repository.OutageRepository
import com.aliJafari.bbarq.isServiceRunning
import com.aliJafari.bbarq.ui.auth.LoginActivity
import com.aliJafari.bbarq.ui.theme.BBarqTheme
import com.aliJafari.bbarq.utils.BillIDNot13Chars
import com.aliJafari.bbarq.utils.BillIDNotFoundException
import com.aliJafari.bbarq.utils.RequestUnsuccessful
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var prefs: SharedPreferences

    private var billId by mutableStateOf("")
    private var billIdError by mutableStateOf<String?>(null)
    private var reminderEnabled by mutableStateOf(false)
    private var outages by mutableStateOf<List<Outage>>(emptyList())
    private var emptyMessage by mutableStateOf<String?>(null)
    private var networkError by mutableStateOf<String?>(null)
    private var isLoading by mutableStateOf(false)
    private var canPostNotifications by mutableStateOf(false)
    private var needsBatteryOptimizationPermission by mutableStateOf(false)
    private var needsExactAlarmPermission by mutableStateOf(false)
    private var serviceRunning by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        prefs = applicationContext.getSharedPreferences("my_prefs", MODE_PRIVATE)

        testToken()
        canPostNotifications = hasNotificationPermission()
        billId = prefs.getString("billId", "").orEmpty()
        reminderEnabled = canPostNotifications && prefs.getBoolean("reminder", false)
        checkPermissions()
        updateServiceState()

        setContent {
            BBarqTheme {
                MainScreen(
                    billId = billId,
                    billIdError = billIdError,
                    reminderEnabled = reminderEnabled,
                    outages = outages,
                    emptyMessage = emptyMessage,
                    networkError = networkError,
                    isLoading = isLoading,
                    needsBatteryOptimizationPermission = needsBatteryOptimizationPermission,
                    needsExactAlarmPermission = needsExactAlarmPermission,
                    serviceRunning = serviceRunning,
                    onBillIdChange = ::onBillIdChange,
                    onReminderChange = ::onReminderChange,
                    onRefreshClick = ::requestCurrentData,
                    onBatteryPermissionClick = ::openBatteryOptimizationSettings,
                    onExactAlarmPermissionClick = ::openExactAlarmSettings,
                    onServiceFabClick = ::toggleService,
                    onAboutClick = ::openAbout,
                    onLogoutClick = ::logout
                )
            }
        }

        if (billId.length == BILL_ID_LENGTH) {
            requestCurrentData()
        } else if (billId.isNotEmpty()) {
            billIdError = getString(R.string.field_error_invalid_id_count)
        }
    }

    override fun onResume() {
        super.onResume()
        canPostNotifications = hasNotificationPermission()
        reminderEnabled = canPostNotifications && prefs.getBoolean("reminder", false)
        checkPermissions()
        updateServiceState()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST) {
            canPostNotifications = hasNotificationPermission()
            reminderEnabled = canPostNotifications && prefs.getBoolean("reminder", false)
        }
    }

    private fun onBillIdChange(value: String) {
        billId = value.filter(Char::isDigit).take(BILL_ID_LENGTH)
        when (billId.length) {
            BILL_ID_LENGTH -> {
                billIdError = null
                prefs.edit(commit = true) { putString("billId", billId) }
                requestCurrentData()
            }
            0 -> billIdError = null
            else -> billIdError = getString(R.string.field_error_invalid_id_count)
        }
    }

    private fun onReminderChange(enabled: Boolean) {
        if (canPostNotifications) {
            reminderEnabled = enabled
            prefs.edit(commit = true) { putBoolean("reminder", enabled) }
        } else {
            askNotificationPermission()
        }
    }

    private fun toggleService() {
        val intent = Intent(this, ForegroundService::class.java)
        if (serviceRunning) {
            stopService(intent)
        } else {
            if (billIdError != null || billId.length != BILL_ID_LENGTH || isLoading) return
            if (canPostNotifications) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            } else {
                askNotificationPermission()
            }
        }
        lifecycleScope.launch {
            delay(100)
            updateServiceState()
        }
    }

    private fun askNotificationPermission() {
        canPostNotifications = hasNotificationPermission()
        if (canPostNotifications) return
        if (!shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            @SuppressLint("InlinedApi")
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST
            )
        } else {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            Toast.makeText(
                this,
                getString(R.string.notification_permission_sub),
                Toast.LENGTH_SHORT
            ).show()
            startActivity(intent)
        }
    }

    private fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun testToken() {
        if (AuthStorage(this).getToken() == null) {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun requestCurrentData() {
        if (isLoading || billId.length != BILL_ID_LENGTH) return
        isLoading = true
        emptyMessage = null
        networkError = null
        lifecycleScope.launch {
            try {
                val schedules = withContext(Dispatchers.IO) {
                    var result: List<Outage> = emptyList()
                    OutageRepository(applicationContext).sendRequest(billId) {
                        result = it
                    }
                    result
                }
                outages = schedules
                billIdError = null
                if (schedules.isEmpty()) {
                    emptyMessage = resources.getStringArray(R.array.no_power_cut_messages).random()
                }
            } catch (e: BillIDNot13Chars) {
                billIdError = getString(R.string.field_error_invalid_id_count)
            } catch (e: BillIDNotFoundException) {
                billIdError = getString(R.string.field_error_invalid_id)
                networkError = getString(R.string.field_error_invalid_id_sub)
            } catch (e: RequestUnsuccessful) {
                networkError = e.details
            } finally {
                isLoading = false
            }
        }
    }

    @SuppressLint("BatteryLife")
    private fun checkPermissions() {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        needsExactAlarmPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            !alarmManager.canScheduleExactAlarms()
        } else {
            false
        }

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        needsBatteryOptimizationPermission =
            !powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    @SuppressLint("BatteryLife")
    private fun openBatteryOptimizationSettings() {
        needsBatteryOptimizationPermission = false
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = "package:$packageName".toUri()
        }
        startActivity(intent)
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            needsExactAlarmPermission = false
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        }
    }

    private fun updateServiceState() {
        Handler(Looper.getMainLooper()).post {
            serviceRunning = isServiceRunning(this, ForegroundService::class.java)
        }
    }

    private fun openAbout() {
        val browserIntent =
            Intent(Intent.ACTION_VIEW, "https://github.com/alijafari-gd/B-Barq".toUri())
        startActivity(browserIntent)
    }

    private fun logout() {
        AuthStorage(this).clearToken()
        startActivity(Intent(this, LoginActivity::class.java))
    }

    companion object {
        private const val BILL_ID_LENGTH = 13
        private const val NOTIFICATION_PERMISSION_REQUEST = 1002
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    billId: String,
    billIdError: String?,
    reminderEnabled: Boolean,
    outages: List<Outage>,
    emptyMessage: String?,
    networkError: String?,
    isLoading: Boolean,
    needsBatteryOptimizationPermission: Boolean,
    needsExactAlarmPermission: Boolean,
    serviceRunning: Boolean,
    onBillIdChange: (String) -> Unit,
    onReminderChange: (Boolean) -> Unit,
    onRefreshClick: () -> Unit,
    onBatteryPermissionClick: () -> Unit,
    onExactAlarmPermissionClick: () -> Unit,
    onServiceFabClick: () -> Unit,
    onAboutClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_more_vert),
                            contentDescription = stringResource(R.string.menu_more)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_logout)) },
                            onClick = {
                                menuExpanded = false
                                onLogoutClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_about)) },
                            onClick = {
                                menuExpanded = false
                                onAboutClick()
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onServiceFabClick,
                icon = {
                    Icon(
                        painter = painterResource(
                            if (serviceRunning) R.drawable.ic_pause else R.drawable.ic_play
                        ),
                        contentDescription = null
                    )
                },
                text = {
                    Text(
                        stringResource(
                            if (serviceRunning) R.string.stop_service_fab else R.string.start_service_fab
                        )
                    )
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                BillIdInput(
                    billId = billId,
                    billIdError = billIdError,
                    onBillIdChange = onBillIdChange
                )
            }
            item {
                ReminderRow(
                    checked = reminderEnabled,
                    onCheckedChange = onReminderChange
                )
            }
            if (needsBatteryOptimizationPermission) {
                item {
                    PermissionWarningCard(
                        title = stringResource(R.string.allow_background_battery_usage),
                        subtitle = stringResource(R.string.battery_permissoin_subtitle),
                        onClick = onBatteryPermissionClick
                    )
                }
            }
            if (needsExactAlarmPermission) {
                item {
                    PermissionWarningCard(
                        title = stringResource(R.string.allow_setting_exact_alarms),
                        subtitle = stringResource(R.string.alarm_permissoin_subtitle),
                        onClick = onExactAlarmPermissionClick
                    )
                }
            }
            item {
                ScheduleHeader(onRefreshClick = onRefreshClick)
            }
            if (isLoading) {
                item {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
            if (networkError != null) {
                item {
                    NetworkErrorCard(message = networkError)
                }
            }
            if (emptyMessage != null) {
                item {
                    Text(
                        text = emptyMessage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = MaterialTheme.typography.headlineSmall.lineHeight * 1.4f
                    )
                }
            }
            items(outages, key = { it.id }) { outage ->
                OutageCard(outage = outage)
            }
        }
    }
}

@Composable
private fun BillIdInput(
    billId: String,
    billIdError: String?,
    onBillIdChange: (String) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_bill),
            contentDescription = null,
            modifier = Modifier.size(35.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(13.dp))
        OutlinedTextField(
            value = billId,
            onValueChange = onBillIdChange,
            modifier = Modifier.weight(1f),
            label = { Text(stringResource(R.string.bill_id_text_input_title)) },
            isError = billIdError != null,
            supportingText = billIdError?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}

@Composable
private fun ReminderRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_notification),
            contentDescription = null,
            modifier = Modifier.size(35.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 13.dp)
        ) {
            Text(
                text = stringResource(R.string.reminder_switch_title),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = stringResource(R.string.reminder_switch_subtitle),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PermissionWarningCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_error_24),
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onClick) {
                Icon(
                    painter = painterResource(R.drawable.outline_arrow_forward_ios_24),
                    contentDescription = stringResource(R.string.permission_request_cta_text),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun ScheduleHeader(onRefreshClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.current_schedule_title),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge
        )
        IconButton(onClick = onRefreshClick) {
            Icon(
                painter = painterResource(R.drawable.ic_renew),
                contentDescription = stringResource(R.string.refresh),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun NetworkErrorCard(message: String) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(Modifier.padding(horizontal = 15.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.baseline_error_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = stringResource(R.string.network_request_failed),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun OutageCard(outage: Outage) {
    val fallback = stringResource(R.string.value_not_available)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(horizontal = 15.dp, vertical = 10.dp)) {
            Text(
                text = outage.date ?: fallback,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            HorizontalDivider(Modifier.padding(vertical = 7.dp))
            Row(Modifier.fillMaxWidth()) {
                ScheduleTimeColumn(
                    title = stringResource(R.string.schedule_layout_start_title),
                    time = outage.startTime ?: fallback,
                    modifier = Modifier.weight(1f)
                )
                ScheduleTimeColumn(
                    title = stringResource(R.string.schedule_layout_end_title),
                    time = outage.endTime ?: fallback,
                    modifier = Modifier.weight(1f)
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 7.dp))
            OutageDetailRow(
                label = stringResource(R.string.schedule_layout_reason_title),
                value = outage.reason ?: fallback
            )
            OutageDetailRow(
                label = stringResource(R.string.schedule_layout_billid_title),
                value = outage.billId ?: fallback
            )
            OutageDetailRow(
                label = stringResource(R.string.address),
                value = outage.address ?: fallback
            )
        }
    }
}

@Composable
private fun ScheduleTimeColumn(
    title: String,
    time: String,
    modifier: Modifier = Modifier
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = time,
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OutageDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 3.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(15.dp))
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
