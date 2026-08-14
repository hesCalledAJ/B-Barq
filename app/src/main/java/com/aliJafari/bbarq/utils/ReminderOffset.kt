package com.aliJafari.bbarq.utils

import com.aliJafari.bbarq.R


enum class ReminderOffset(val minutes: Int, val bit: Int, val labelRes: Int) {
    TWO_HOURS(120, 1, R.string.reminder_2h),
    ONE_HOUR(60, 2, R.string.reminder_1h),
    THIRTY_MIN(30, 4, R.string.reminder_30m),
    FIVE_MIN(5, 8, R.string.reminder_5m)
}