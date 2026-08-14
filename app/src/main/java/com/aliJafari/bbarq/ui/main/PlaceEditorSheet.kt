package com.aliJafari.bbarq.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aliJafari.bbarq.R
import com.aliJafari.bbarq.data.model.Place
import com.aliJafari.bbarq.utils.ReminderOffset


private val placeColorOptions = listOf(
    ColorOption("blue", Color(0xFF4C6EF5)),
    ColorOption("red", Color(0xFFCE1A2F)),
    ColorOption("teal", Color(0xFF12B886)),
    ColorOption("amber", Color(0xFFF59F00)),
    ColorOption("idk", Color(0xFFE64980)),
    ColorOption("purple", Color(0xFF9775FA)),
    ColorOption("green", Color(0xFF66A80F)),
    ColorOption("cyan", Color(0xFF15AABF)),
    ColorOption("gray", Color(0xFF868E96)),
    ColorOption("white", Color(0xFFF3F3F3))
)

private val placeIconOptions = listOf(
    IconOption("home", R.drawable.ic_home),
    IconOption("business", R.drawable.ic_work),
    IconOption("apartment", R.drawable.ic_apartment),
    IconOption("university", R.drawable.ic_university),
    IconOption("store", R.drawable.ic_shop),
    IconOption("fridge", R.drawable.ic_fridge),
    IconOption("game", R.drawable.ic_game),
    IconOption("warehouse", R.drawable.ic_garage),
    IconOption("server", R.drawable.ic_server)
)

data class ColorOption(val key: String, val color: Color)
data class IconOption(val key: String, val icon: Int)

@Composable
fun PlaceEditorSheet(
    place: Place?,
    onCancel: () -> Unit,
    onSave: (Place) -> Unit,
) {
    var name by remember(place) { mutableStateOf(place?.name.orEmpty()) }
    var billId by remember(place) { mutableStateOf(place?.billId.orEmpty()) }
    var colorKey by remember(place) {
        mutableStateOf(
            place?.colorKey ?: placeColorOptions.first().key
        )
    }
    var iconKey by remember(place) {
        mutableStateOf(
            place?.iconKey ?: placeIconOptions.first().key
        )
    }
    var remindersEnabled by remember(place) { mutableStateOf(place?.remindersEnabled ?: false) }
    var reminderMask by remember(place) { mutableIntStateOf(place?.reminderOffsetsMask ?: 0) }

    val nameError = name.isBlank()
    val billIdError = billId.length != BILL_ID_LENGTH
    val canSave = !nameError && !billIdError

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = stringResource(if (place == null) R.string.add_place_title else R.string.edit_place_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.place_name)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            isError = nameError && name.isNotEmpty()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = billId,
            onValueChange = {
                billId = it
                    .replace('٠', '0').replace('١', '1').replace('٢', '2').replace('٣', '3')
                    .replace('٤', '4')
                    .replace('٥', '5').replace('٦', '6').replace('٧', '7').replace('٨', '8')
                    .replace('٩', '9')
                    .replace('۰', '0').replace('۱', '1').replace('۲', '2').replace('۳', '3')
                    .replace('۴', '4') // persian/urdu variant
                    .replace('۵', '5').replace('۶', '6').replace('۷', '7').replace('۸', '8')
                    .replace('۹', '9')
                    .filter(Char::isDigit).take(BILL_ID_LENGTH)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.place_bill_id)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = billId.isNotEmpty() && billIdError,
            supportingText = if (billId.isNotEmpty() && billIdError) {
                { Text(stringResource(R.string.field_error_invalid_id_count)) }
            } else null
        )

        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.place_color),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            placeColorOptions.forEach { option ->
                val selected = colorKey == option.key
                val size by animateDpAsState(if (selected) 44.dp else 36.dp, label = "swatchSize")
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { colorKey = option.key },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(size)
                            .clip(CircleShape)
                            .background(option.color)
                    )
                    if (selected) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.place_icon),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            placeIconOptions.forEach { option ->
                val selected = iconKey == option.key
                val bg by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    label = "iconBg"
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(bg)
                        .clickable { iconKey = option.key },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(option.icon),
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.place_reminders_switch_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.place_reminders_switch_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = remindersEnabled,
                onCheckedChange = { checked ->
                    remindersEnabled = checked
                    if (!checked) reminderMask = 0
                }
            )
        }
        AnimatedVisibility(visible = remindersEnabled) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 15.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReminderOffset.entries.chunked(2).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowOptions.forEach { offset ->
                            val checked = reminderMask and offset.bit != 0
                            FilterChip(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                selected = checked,
                                onClick = {
                                    reminderMask = reminderMask xor offset.bit
                                    if (reminderMask == 0) remindersEnabled = false
                                },
                                label = {
                                    Text(
                                        text = stringResource(offset.labelRes),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                },
                                leadingIcon = if (checked) {
                                    {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            Modifier.size(18.dp)
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
            Spacer(Modifier.width(8.dp))
            Button(
                enabled = canSave,
                onClick = {
                    onSave(
                        Place(
                            id = place?.id ?: 0,
                            name = name.trim(),
                            billId = billId,
                            colorKey = colorKey,
                            iconKey = iconKey,
                            reminderOffsetsMask = if (remindersEnabled) reminderMask else 0
                        )
                    )
                }
            ) { Text(stringResource(R.string.save_button_text)) }
        }
        Spacer(Modifier.height(18.dp))
    }
}

fun placeColorOption(key: String): ColorOption =
    placeColorOptions.firstOrNull { it.key == key } ?: placeColorOptions.first()

fun placeIconOption(key: String): IconOption =
    placeIconOptions.firstOrNull { it.key == key } ?: placeIconOptions.first()

private const val BILL_ID_LENGTH = 13