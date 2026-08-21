package com.aliJafari.bbarq.utils

import androidx.appcompat.app.AppCompatDelegate

fun String.toPersianDigitsIfNeeded(): String {
    val locale = AppCompatDelegate.getApplicationLocales().get(0)
    if (locale?.language != "fa") return this
    val persianDigits = "۰۱۲۳۴۵۶۷۸۹"
    return map { c -> if (c in '0'..'9') persianDigits[c - '0'] else c }.joinToString("")
}