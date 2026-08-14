package com.aliJafari.bbarq.data.local

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import com.aliJafari.bbarq.ui.main.AppLanguage

class PreferencesManager(context: Context) {
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    fun getDarkMode(def : Boolean): Boolean = prefs.getBoolean("dark_mode", def)
    fun setDarkMode(enabled: Boolean) = prefs.edit { putBoolean("dark_mode", enabled) }

    fun getLanguage(): AppLanguage =
        AppLanguage.valueOf(prefs.getString("language", AppLanguage.EN.name)!!)

    fun setLanguage(lang: AppLanguage) = prefs.edit {
        putString("language", lang.name)
    }

}