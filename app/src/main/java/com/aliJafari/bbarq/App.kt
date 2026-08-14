package com.aliJafari.bbarq

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.aliJafari.bbarq.data.local.PreferencesManager
import io.appmetrica.analytics.AppMetrica
import io.appmetrica.analytics.AppMetricaConfig

class App : Application() {
    lateinit var prefsManager : PreferencesManager
    override fun onCreate() {
        super.onCreate()
        prefsManager = PreferencesManager(this)
        val saved = prefsManager.getLanguage()
        changeLanguage(this,saved.name)
        val config = AppMetricaConfig.newConfigBuilder("8e651fd5-277a-45a6-852f-ecd23aefbb92").build()
        AppMetrica.activate(this, config)
    }

    fun changeLanguage(context: Context, language: String) {
        val localeList = LocaleListCompat.forLanguageTags("fa")
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}
