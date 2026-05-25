package com.anegan.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {
    private const val GITHUB_RELEASES_URL = "https://api.github.com/repos/heisenricher/Anegan/releases/latest"

    suspend fun getLatestReleaseVersion(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(GITHUB_RELEASES_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val tagName = json.getString("tag_name")
                    // Strip the 'v' prefix if present
                    if (tagName.startsWith("v", ignoreCase = true)) {
                        tagName.substring(1)
                    } else {
                        tagName
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun isUpdateAvailable(currentVersion: String, latestVersion: String): Boolean {
        try {
            val currentParts = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }
            val latestParts = latestVersion.split(".").map { it.toIntOrNull() ?: 0 }

            val maxLength = maxOf(currentParts.size, latestParts.size)
            for (i in 0 until maxLength) {
                val current = currentParts.getOrElse(i) { 0 }
                val latest = latestParts.getOrElse(i) { 0 }
                if (latest > current) return true
                if (current > latest) return false
            }
            return false
        } catch (e: Exception) {
            return false
        }
    }
}
