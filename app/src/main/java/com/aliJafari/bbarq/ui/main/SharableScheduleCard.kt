package com.aliJafari.bbarq.ui.main

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aliJafari.bbarq.R
import com.aliJafari.bbarq.data.repository.PlaceOutage

@Composable
fun ShareableScheduleCard(
    schedule: PlaceOutage,
) {
    val colorOption = placeColorOption(schedule.place.colorKey)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(colorOption.color)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        horizontal = 14.dp,
                        vertical = 14.dp
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(
                            placeIconOption(
                                schedule.place.iconKey
                            ).icon
                        ),
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

                    StatusBadge(
                        status = relativeStatus(
                            schedule.outage,
                            LocalContext.current
                        )
                    )
                }

                Spacer(Modifier.height(8.dp))

                ScheduleMetaRow(
                    iconRes = R.drawable.ic_calendar,
                    text = schedule.outage.date
                        ?: stringResource(R.string.value_not_available)
                )

                ScheduleMetaRow(
                    iconRes = R.drawable.ic_clock,
                    text = stringResource(
                        R.string.schedule_time_range,
                        schedule.outage.startTime
                            ?: stringResource(R.string.value_not_available),
                        schedule.outage.endTime
                            ?: stringResource(R.string.value_not_available)
                    )
                )

                ScheduleMetaRow(
                    iconRes = R.drawable.ic_bolt,
                    text = schedule.outage.reason
                        ?: stringResource(R.string.value_not_available)
                )

                ScheduleMetaRow(
                    iconRes = R.drawable.ic_location,
                    text = schedule.outage.address
                        ?: stringResource(R.string.value_not_available)
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "github.com/hesCalledAJ/B-Barq",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}