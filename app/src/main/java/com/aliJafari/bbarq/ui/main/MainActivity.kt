package com.aliJafari.bbarq.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Context
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
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import com.aliJafari.bbarq.App
import com.aliJafari.bbarq.ForegroundService
import com.aliJafari.bbarq.R
import com.aliJafari.bbarq.data.local.AuthStorage
import com.aliJafari.bbarq.data.local.PreferencesManager
import com.aliJafari.bbarq.data.model.Outage
import com.aliJafari.bbarq.data.model.Place
import com.aliJafari.bbarq.data.repository.OutageRepository
import com.aliJafari.bbarq.data.repository.PlaceOutage
import com.aliJafari.bbarq.data.repository.PlaceRepository
import com.aliJafari.bbarq.isServiceRunning
import com.aliJafari.bbarq.ui.auth.LoginActivity
import com.aliJafari.bbarq.ui.theme.BBarqTheme
import com.aliJafari.bbarq.utils.BillIDNot13Chars
import com.aliJafari.bbarq.utils.BillIDNotFoundException
import com.aliJafari.bbarq.utils.RequestUnsuccessful
import com.aliJafari.bbarq.utils.shareSchedule
import com.aliJafari.bbarq.utils.toEpochMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import saman.zamani.persiandate.PersianDate
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var placeRepository: PlaceRepository
    private lateinit var prefsManager: PreferencesManager


    private var selectedTab by mutableStateOf(MainTab.Schedules)
    private var places by mutableStateOf<List<Place>>(emptyList())
    private var placeOutages by mutableStateOf<List<PlaceOutage>>(emptyList())
    private var emptyMessage by mutableStateOf<String?>(null)
    private var errorMessages by mutableStateOf<String>("")
    private var failedPlaces by mutableStateOf<List<Place>>(emptyList())
    private var isLoading by mutableStateOf(false)
    private var hasNotificationPermission by mutableStateOf(false)
    private var needsBatteryOptimizationPermission by mutableStateOf(false)
    private var needsExactAlarmPermission by mutableStateOf(false)
    private var serviceRunning by mutableStateOf(false)
    private var editingPlace by mutableStateOf<Place?>(null)
    private var showPlaceSheet by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        prefs = applicationContext.getSharedPreferences("my_prefs", MODE_PRIVATE)
        placeRepository = PlaceRepository(applicationContext)
        prefsManager = (application as App).prefsManager
        applyLanguage(prefsManager.getLanguage())
        testToken()
        hasNotificationPermission = hasNotificationPermission()
        checkPermissions()
        updateServiceState()

        setContent {
            val shareScope = rememberCoroutineScope()
            val systemDarkThemeEnabled = isSystemInDarkTheme()
            var darkModeEnabled by remember { mutableStateOf(prefsManager.getDarkMode(systemDarkThemeEnabled)) }
            var language by remember { mutableStateOf(prefsManager.getLanguage()) }

            BBarqTheme(darkTheme = darkModeEnabled) {
                MainScreen(
                    selectedTab = selectedTab,
                    places = places,
                    placeOutages = placeOutages,
                    emptyMessage = emptyMessage,
                    errorMessages = errorMessages,
                    failedPlaces = failedPlaces,
                    isLoading = isLoading,
                    canStartService = places.isNotEmpty() && !isLoading,
                    serviceRunning = serviceRunning,
                    needsPostNotificationPermission = !hasNotificationPermission,
                    needsBatteryOptimizationPermission = needsBatteryOptimizationPermission,
                    needsExactAlarmPermission = needsExactAlarmPermission,
                    showPlaceSheet = showPlaceSheet,
                    editingPlace = editingPlace,
                    language = language,
                    shareScope = shareScope,
                    darkModeEnabled = darkModeEnabled,
                    onTabSelected = { selectedTab = it },
                    onRefreshClick = { requestCurrentData(this) },
                    onServiceFabClick = ::toggleService,
                    onNotificationPermissionClick = ::askNotificationPermission,
                    onBatteryPermissionClick = ::openBatteryOptimizationSettings,
                    onExactAlarmPermissionClick = ::openExactAlarmSettings,
                    onAddPlaceClick = {
                        selectedTab = MainTab.Preferences
                        editingPlace = null
                        showPlaceSheet = true
                    },
                    onEditPlaceClick = {
                        editingPlace = it
                        showPlaceSheet = true
                    },
                    onDeletePlaceClick = ::deletePlace,
                    onDismissPlaceSheet = { showPlaceSheet = false },
                    onSavePlace = ::savePlace,
                    onAboutClick = ::openAbout,
                    onLogoutClick = ::logout,
                    onDarkModeChange = {
                        darkModeEnabled = it
                        prefsManager.setDarkMode(it)
                    },
                    onLanguageChange = {
                        language = it
                        prefsManager.setLanguage(language)
                        applyLanguage(it)
                    }
                )
            }
        }

        loadPlaces(this)
    }

    private fun applyLanguage(language: AppLanguage) {
        val localeList = LocaleListCompat.forLanguageTags(language.name.lowercase(Locale.US))
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    override fun onResume() {
        super.onResume()
        hasNotificationPermission = hasNotificationPermission()
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
            hasNotificationPermission = hasNotificationPermission()
        }
    }

    private fun loadPlaces(c: Context) {
        lifecycleScope.launch {
            places = withContext(Dispatchers.IO) { placeRepository.getPlaces() }
            requestCurrentData(c)
        }
    }

    private fun savePlace(place: Place) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { placeRepository.savePlace(place) }
            showPlaceSheet = false
            loadPlaces(this@MainActivity)
        }
    }

    private fun deletePlace(place: Place) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { placeRepository.deletePlace(place) }
            loadPlaces(this@MainActivity)
        }
    }

    private fun toggleService() {
        val intent = Intent(this, ForegroundService::class.java)
        if (serviceRunning) {
            stopService(intent)
        } else {
            if (places.isEmpty() || isLoading) return
            if (hasNotificationPermission) {
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

    private fun requestCurrentData(c: Context) {
        if (isLoading) return
        if (places.isEmpty()) {
            placeOutages = emptyList()
            emptyMessage = getString(R.string.empty_places_message)
            return
        }
        isLoading = true
        emptyMessage = null
        errorMessages = ""
        failedPlaces = emptyList()
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                val repository = OutageRepository(applicationContext)
                val schedules = mutableListOf<PlaceOutage>()
                val errors = mutableListOf<Place>()
                val messages = mutableListOf<String>()
                places.forEach { place ->
                    try {
                        repository.fetchOutages(place.billId).forEach { outage ->
                            schedules.add(PlaceOutage(place = place, outage = outage))
                        }
                    } catch (e: BillIDNot13Chars) {
                        errors.add(place)
                    } catch (e: BillIDNotFoundException) {
                        errors.add(place)
                        messages += c.getString(R.string.place_fetch_invalid_bill_id, place.name)
                    } catch (e: RequestUnsuccessful) {
                        errors.add(place)
                        messages += c.getString(R.string.place_fetch_failed, place.name, e.details)
                    }
                }
                errorMessages = messages.joinToString("\n")
                schedules to errors
            }

            placeOutages = result.first
            failedPlaces = result.second
            emptyMessage = if (placeOutages.isEmpty()) {
                resources.getStringArray(R.array.no_power_cut_messages).random()
            } else null
            isLoading = false
        }
    }

    private fun askNotificationPermission() {
        hasNotificationPermission = hasNotificationPermission()
        if (hasNotificationPermission) return
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
            finish()
        }
    }

    @SuppressLint("BatteryLife")
    private fun checkPermissions() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        needsBatteryOptimizationPermission = !powerManager.isIgnoringBatteryOptimizations(packageName)

        if (!needsBatteryOptimizationPermission){
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            needsExactAlarmPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                !alarmManager.canScheduleExactAlarms()
            } else {
                false
            }
        }
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
        startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/alijafari-gd/B-Barq".toUri()))
    }

    private fun logout() {
        AuthStorage(this).clearToken()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST = 1002
    }
}

private enum class MainTab {
    Schedules,
    Preferences
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    selectedTab: MainTab,
    places: List<Place>,
    placeOutages: List<PlaceOutage>,
    emptyMessage: String?,
    errorMessages: String,
    failedPlaces: List<Place>?,
    isLoading: Boolean,
    canStartService: Boolean,
    serviceRunning: Boolean,
    needsPostNotificationPermission: Boolean,
    needsBatteryOptimizationPermission: Boolean,
    needsExactAlarmPermission: Boolean,
    darkModeEnabled: Boolean,
    language: AppLanguage,
    showPlaceSheet: Boolean,
    editingPlace: Place?,
    shareScope: CoroutineScope,
    onTabSelected: (MainTab) -> Unit,
    onRefreshClick: () -> Unit,
    onServiceFabClick: () -> Unit,
    onNotificationPermissionClick: () -> Unit,
    onBatteryPermissionClick: () -> Unit,
    onExactAlarmPermissionClick: () -> Unit,
    onAddPlaceClick: () -> Unit,
    onEditPlaceClick: (Place) -> Unit,
    onDeletePlaceClick: (Place) -> Unit,
    onDismissPlaceSheet: () -> Unit,
    onSavePlace: (Place) -> Unit,
    onAboutClick: () -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onLogoutClick: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (selectedTab == MainTab.Schedules) {
                                R.string.upcoming_outages
                            } else {
                                R.string.preferences_title
                            }
                        )
                    )
                },
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
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == MainTab.Schedules,
                    onClick = { onTabSelected(MainTab.Schedules) },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_schedule_tab),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text(stringResource(R.string.schedules_tab)) }
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.Preferences,
                    onClick = { onTabSelected(MainTab.Preferences) },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_prefs),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text(stringResource(R.string.prefs_tab)) }
                )
            }
        },
        floatingActionButton = {
            when (selectedTab) {
                MainTab.Schedules -> ExtendedFloatingActionButton(
                    onClick = onServiceFabClick,
                    expanded = true,
                    icon = {
                        Icon(
                            painter = painterResource(
                                if (serviceRunning) R.drawable.ic_pause else R.drawable.ic_play
                            ),
                            contentDescription = stringResource(
                                if (serviceRunning) R.string.stop_service_fab else R.string.start_service_fab
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    text = { Text(stringResource(if (serviceRunning) R.string.stop_service_fab else R.string.start_service_fab)) }
                )

                MainTab.Preferences -> {}
            }
        }
    ) { padding ->
        if (selectedTab == MainTab.Schedules) {
            SchedulesTab(
                placeOutages = placeOutages,
                emptyMessage = emptyMessage,
                errorMessages = errorMessages,
                failedPlaces = failedPlaces,
                isLoading = isLoading,
                canStartService = canStartService,
                contentPadding = padding,
                onRefreshClick = onRefreshClick,
                isPlacesListEmpty = places.isEmpty(),
                onManagePlacesClick = onAddPlaceClick,
                shareScope = shareScope
            )
        } else {
            PreferencesTab(
                places = places,
                needsPostNotificationPermission = needsPostNotificationPermission,
                needsBatteryOptimizationPermission = needsBatteryOptimizationPermission,
                needsExactAlarmPermission = needsExactAlarmPermission,
                contentPadding = padding,
                onBatteryPermissionClick = onBatteryPermissionClick,
                onNotificationPermissionClick = onNotificationPermissionClick,
                onExactAlarmPermissionClick = onExactAlarmPermissionClick,
                onEditPlaceClick = onEditPlaceClick,
                onDeletePlaceClick = onDeletePlaceClick,
                darkModeEnabled = darkModeEnabled,
                language = language,
                onDarkModeChange = onDarkModeChange,
                onLanguageChange = onLanguageChange,
                onLogoutClick = onLogoutClick,
                onAddPlaceClick = onAddPlaceClick
            )
        }
    }

    if (showPlaceSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismissPlaceSheet,
            sheetState = sheetState
        ) {
            PlaceEditorSheet(
                place = editingPlace,
                onCancel = onDismissPlaceSheet,
                onSave = onSavePlace
            )
        }
    }
}

@Composable
private fun SchedulesTab(
    placeOutages: List<PlaceOutage>,
    emptyMessage: String?,
    errorMessages: String,
    failedPlaces: List<Place>?,
    isLoading: Boolean,
    canStartService: Boolean,
    contentPadding: PaddingValues,
    isPlacesListEmpty : Boolean,
    shareScope : CoroutineScope,
    onRefreshClick: () -> Unit,
    onManagePlacesClick: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            if (isLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            } else {
                Spacer(Modifier.height(2.dp))
            }
        }
        if (failedPlaces.isNullOrEmpty().not()) {
            item {
                NetworkErrorCard( errorMessages)
            }
        } else if (emptyMessage != null) {
            item {
                EmptySchedules(
                    message = emptyMessage,
                    canStartService = canStartService,
                    onManagePlacesClick = onManagePlacesClick
                )
            }
        }
        items((placeOutages).sortedBy { it.outage.toEpochMillis() }, key = { "${it.place.id}-${it.outage.id}" }) { schedule ->
            ScheduleCard(schedule = schedule, shareScope = shareScope)
        }
        if (!isPlacesListEmpty) item {
            TextButton(onClick = onRefreshClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.refresh))
            }
        }
    }
}

@Composable
private fun EmptySchedules(
    message: String,
    canStartService: Boolean,
    onManagePlacesClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        if (!canStartService) {
            Spacer(Modifier.height(10.dp))
            Button(onClick = onManagePlacesClick) {
                Text(stringResource(R.string.add_first_place))
            }
        }
    }
}


@Composable
private fun NetworkErrorCard(error: String) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            Modifier
                .padding(horizontal = 15.dp, vertical = 10.dp)
                .fillMaxWidth()
        ) {
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
            if (error.isBlank().not()) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun PlacePill(place: Place) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(
            painter = painterResource(placeIconOption(place.iconKey).icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = place.name,
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun PermissionWarningCard(
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

fun daysBetween(from: Calendar, to: Calendar): Int {
    val a = from.clone() as Calendar
    a.set(Calendar.HOUR_OF_DAY, 0); a.set(Calendar.MINUTE, 0); a.set(Calendar.SECOND, 0); a.set(
        Calendar.MILLISECOND,
        0
    )
    val b = to.clone() as Calendar
    b.set(Calendar.HOUR_OF_DAY, 0); b.set(Calendar.MINUTE, 0); b.set(Calendar.SECOND, 0); b.set(
        Calendar.MILLISECOND,
        0
    )
    return ((b.timeInMillis - a.timeInMillis) / 86400000).toInt()
}
@Composable
private fun ScheduleCard(schedule: PlaceOutage,shareScope : CoroutineScope) {
    val colorOption = placeColorOption(schedule.place.colorKey)
    val c = LocalContext.current
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(colorOption.color)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(placeIconOption(schedule.place.iconKey).icon),
                        modifier = Modifier.size(24.dp),
                        contentDescription = null
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = schedule.place.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    StatusBadge(status = relativeStatus(schedule.outage,LocalContext.current))
                }
                Spacer(Modifier.height(8.dp))
                ScheduleMetaRow(
                    iconRes = R.drawable.ic_calendar,
                    text = schedule.outage.date?:stringResource(R.string.value_not_available)
                )
                ScheduleMetaRow(
                    iconRes = R.drawable.ic_clock,
                    text = stringResource(
                        R.string.schedule_time_range,
                        schedule.outage.startTime?.takeIf { it.isNotBlank() } ?: stringResource(R.string.value_not_available),
                        schedule.outage.endTime?.takeIf { it.isNotBlank() } ?: stringResource(R.string.value_not_available)
                    )
                )
                ScheduleMetaRow(
                    iconRes = R.drawable.ic_bolt,
                    text = schedule.outage.reason ?: stringResource(R.string.value_not_available)
                )
                ScheduleMetaRow(
                    iconRes = R.drawable.ic_location,
                    text = schedule.outage.address ?: stringResource(R.string.value_not_available)
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(7.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(true){
                            shareScope.launch {
                                shareSchedule(c,schedule)
                            }
                        }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.share),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}


@Composable
fun ScheduleMetaRow(
    iconRes: Int,
    text: String,
) {
    Row(
        modifier = Modifier.padding(top = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(17.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
@Composable
fun PlaceRow(
    place: Place,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val colorOption = placeColorOption(place.colorKey)
    val iconOption = placeIconOption(place.iconKey)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colorOption.color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconOption.icon),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp)
            ) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.place_id_line, place.billId),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalIconButton(onClick = onEditClick, modifier = Modifier.size(38.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = stringResource(R.string.edit_place),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(6.dp))
            FilledTonalIconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(38.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.delete_place),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

