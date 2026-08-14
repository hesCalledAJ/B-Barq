package com.aliJafari.bbarq.ui.main

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aliJafari.bbarq.R
import com.aliJafari.bbarq.data.model.Outage
import com.aliJafari.bbarq.utils.toEpochMillis
import java.util.Calendar

enum class ScheduleUrgency { ENDED, ONGOING, SOON, TODAY, UPCOMING }

data class ScheduleStatus(val label: String, val urgency: ScheduleUrgency)

fun relativeStatus(outage: Outage, context: Context): ScheduleStatus {
    val date = outage.date?.takeIf { it.isNotBlank() }
        ?: return ScheduleStatus(context.getString(R.string.value_not_available), ScheduleUrgency.UPCOMING)

    val startTime = outage.startTime?.takeIf { it.isNotBlank() }

    // no time — fall back to day-level status only
    if (startTime == null) {
        val dateOnlyTarget = date.toEpochMillis("00:00")
        if (dateOnlyTarget == -1L) return ScheduleStatus(context.getString(R.string.value_not_available), ScheduleUrgency.UPCOMING)

        val daysDiff = daysBetween(Calendar.getInstance(), Calendar.getInstance().apply { timeInMillis = dateOnlyTarget })
        return when {
            daysDiff < 0 -> ScheduleStatus(context.getString(R.string.status_ended), ScheduleUrgency.ENDED)
            daysDiff == 0 -> ScheduleStatus(context.getString(R.string.today), ScheduleUrgency.SOON)
            daysDiff == 1 -> ScheduleStatus(context.getString(R.string.tomorrow), ScheduleUrgency.TODAY)
            else -> ScheduleStatus(context.getString(R.string.in_days, daysDiff), ScheduleUrgency.UPCOMING)
        }
    }

    val startTarget = date.toEpochMillis(startTime)
    if (startTarget == -1L) return ScheduleStatus(context.getString(R.string.value_not_available), ScheduleUrgency.UPCOMING)

    var endTarget = outage.endTime?.takeIf { it.isNotBlank() }?.let { date.toEpochMillis(it) }?.takeIf { it != -1L }
    if (endTarget != null && endTarget <= startTarget) endTarget += 24 * 60 * 60 * 1000 // end wraps past midnight

    val now = Calendar.getInstance().timeInMillis

    if (endTarget != null && now > endTarget)
        return ScheduleStatus(context.getString(R.string.status_ended), ScheduleUrgency.ENDED)
    if (now >= startTarget)
        return ScheduleStatus(context.getString(R.string.status_ongoing), ScheduleUrgency.ONGOING)

    val minutesDiff = (startTarget - now) / 60000
    val daysDiff = daysBetween(Calendar.getInstance(), Calendar.getInstance().apply { timeInMillis = startTarget })
    return when {
        daysDiff == 0 -> {
            val hours = minutesDiff / 60
            val label = if (hours < 1) context.getString(R.string.in_minutes, minutesDiff)
            else context.getString(R.string.in_hours, hours)
            ScheduleStatus(label, if (hours < 3) ScheduleUrgency.SOON else ScheduleUrgency.TODAY)
        }
        daysDiff == 1 -> ScheduleStatus(context.getString(R.string.tomorrow), ScheduleUrgency.TODAY)
        else -> ScheduleStatus(context.getString(R.string.in_days, daysDiff), ScheduleUrgency.UPCOMING)
    }
}

@Composable
fun StatusBadge(status: ScheduleStatus) {
    val (bg, fg, icon) = when (status.urgency) {
        ScheduleUrgency.ONGOING  -> Triple(Color(0xFFFFD9DE), Color(0xFFC62828), R.drawable.baseline_error_24)
        ScheduleUrgency.SOON     -> Triple(Color(0xFFFFE0B2), Color(0xFFE65100), R.drawable.ic_bolt)
        ScheduleUrgency.TODAY    -> Triple(Color(0xFFFFF3C4), Color(0xFF8D6E00), R.drawable.ic_clock)
        ScheduleUrgency.UPCOMING -> Triple(Color(0xFFDDF3E4), Color(0xFF2E7D32), R.drawable.ic_calendar)
        ScheduleUrgency.ENDED    -> Triple(Color(0xFFE0E0E0), Color(0xFF616161), R.drawable.ic_check)
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(13.dp), tint = fg)
        Spacer(Modifier.width(3.dp))
        Text(status.label, color = fg, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}