package com.aliJafari.bbarq.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.aliJafari.bbarq.R
import com.aliJafari.bbarq.data.model.Place
import com.aliJafari.bbarq.utils.toPersianDigitsIfNeeded


@Composable
fun PreferencesTab(
    places: List<Place>,
    darkModeEnabled: Boolean,
    language: AppLanguage,
    needsPostNotificationPermission: Boolean,
    needsBatteryOptimizationPermission: Boolean,
    needsExactAlarmPermission: Boolean,
    contentPadding: PaddingValues,
    onNotificationPermissionClick: () -> Unit,
    onBatteryPermissionClick: () -> Unit,
    onExactAlarmPermissionClick: () -> Unit,
    onEditPlaceClick: (Place) -> Unit,
    onDeletePlaceClick: (Place) -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onLogoutClick: () -> Unit,
    onAddPlaceClick: () -> Unit,
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        Dialog(
            onDismissRequest = { showLogoutDialog = false }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.action_logout),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = stringResource(R.string.logout_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showLogoutDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.cancel))
                        }

                        Button(
                            onClick = {
                                showLogoutDialog = false
                                onLogoutClick()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text(stringResource(R.string.yes))
                        }
                    }
                }
            }
        }
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (needsPostNotificationPermission) {
            item {
                PermissionWarningCard(
                    title = stringResource(R.string.allow_post_notifications),
                    subtitle = stringResource(R.string.notification_permissoin_subtitle),
                    onClick = onNotificationPermissionClick
                )
            }
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

        item { SectionHeader(stringResource(R.string.places_section_title)) }
        if (places.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.empty_places_message),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        items(places, key = { it.id }) { place ->
            PlaceRow(
                place = place,
                onEditClick = { onEditPlaceClick(place) },
                onDeleteClick = { onDeletePlaceClick(place) }
            )
        }
        item {
            Row {
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onAddPlaceClick
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add_place),
                            contentDescription = stringResource(R.string.add_new_place),
                            modifier = Modifier.size(21.dp)
                        )
                        Text(
                            text = stringResource(R.string.add_new_place),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
            }
        }
        item { SectionHeader(stringResource(R.string.appearance_section_title)) }
        item {
            SettingsToggleRow(
                title = stringResource(R.string.dark_mode_title),
                checked = darkModeEnabled,
                onCheckedChange = onDarkModeChange
            )
        }
        item {
            LanguageSwitchRow(
                language = language,
                onLanguageChange = onLanguageChange
            )
        }

        item { SectionHeader(stringResource(R.string.account_section_title)) }
        item { LogoutCard(onClick = {showLogoutDialog = true}) }
        item { Text(text = stringResource(R.string.action_logout_tip),style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic)) }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun LanguageSwitchRow(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.language_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        SingleChoiceSegmentedButtonRow {
            AppLanguage.entries.forEachIndexed { index, lang ->
                SegmentedButton(
                    selected = language == lang,
                    onClick = { onLanguageChange(lang) },
                    shape = SegmentedButtonDefaults.itemShape(index, AppLanguage.entries.size)
                ) {
                    Text(if (lang == AppLanguage.FA) "فارسی" else "EN")
                }
            }
        }
    }
}

@Composable
private fun LogoutCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_delete),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.action_logout),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.titleMedium
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
                    text = place.name.toPersianDigitsIfNeeded(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.place_id_line, place.billId).toPersianDigitsIfNeeded(),
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

enum class AppLanguage { EN, FA }