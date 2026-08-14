package com.aliJafari.bbarq.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import com.aliJafari.bbarq.BuildConfig

data class UpdateInfo(val latestVersion: String, val downloadUrl: String)

object UpdateChecker {
    private const val REPO = "hesCalledAJ/B-Barq" // owner/repo

    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val json = URL("https://api.github.com/repos/$REPO/releases/latest")
                .readText()
            val obj = JSONObject(json)
            val tag = obj.getString("tag_name").removePrefix("v").removePrefix("V")
            val url = obj.getString("html_url")
            if (isNewer(tag, BuildConfig.VERSION_NAME)) UpdateInfo(tag, url) else null
        } catch (e: Exception) {
            null
        }
    }

    private fun isNewer(remote: String, current: String): Boolean {
        val r = remote.split(".").map { it.toIntOrNull() ?: 0 }
        val c = current.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(r.size, c.size)) {
            val a = r.getOrElse(i) { 0 }
            val b = c.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }
}