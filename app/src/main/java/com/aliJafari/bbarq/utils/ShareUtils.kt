package com.aliJafari.bbarq.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.view.doOnPreDraw
import com.aliJafari.bbarq.R
import com.aliJafari.bbarq.data.repository.PlaceOutage
import com.aliJafari.bbarq.ui.main.ShareableScheduleCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

suspend fun shareSchedule(
    context: Context,
    schedule: PlaceOutage,
) {
    val bitmap = createScheduleBitmap(
        context = context,
        schedule = schedule
    )

    val file = File(
        context.cacheDir,
        "schedule_${System.currentTimeMillis()}.png"
    )

    FileOutputStream(file).use { output ->
        bitmap.compress(
            Bitmap.CompressFormat.PNG,
            100,
            output
        )
    }

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"

        putExtra(
            Intent.EXTRA_TEXT,
            generateShareText(context,schedule)
        )

        putExtra(
            Intent.EXTRA_STREAM,
            uri
        )

        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(
        Intent.createChooser(
            shareIntent,
            context.getString(R.string.share)
        )
    )
}
fun generateShareText(
    context: Context,
    schedule: PlaceOutage
): String {
    return context.getString(
        R.string.share_outage_text,
        schedule.place.name,
        schedule.outage.date
            ?: context.getString(R.string.value_not_available),
        schedule.outage.startTime
            ?: context.getString(R.string.value_not_available),
        schedule.outage.endTime
            ?: context.getString(R.string.value_not_available),
        schedule.outage.address
            ?: context.getString(R.string.value_not_available)
    )
}
private suspend fun createScheduleBitmap(
    context: Context,
    schedule: PlaceOutage,
): Bitmap = suspendCancellableCoroutine { continuation ->

    val activity = context as? Activity
        ?: error("Context must be an Activity")

    val root = activity.window.decorView as ViewGroup

    val composeView = ComposeView(context)

    root.addView(
        composeView,
        ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    )

    composeView.setContent {
        MaterialTheme {
            ShareableScheduleCard(schedule)
        }
    }

    composeView.doOnPreDraw {
        val bitmap = createBitmap(composeView.width, composeView.height)

        Canvas(bitmap).apply {
            composeView.draw(this)
        }

        root.removeView(composeView)

        if (continuation.isActive) {
            continuation.resume(bitmap)
        }
    }

    continuation.invokeOnCancellation {
        root.removeView(composeView)
    }
}