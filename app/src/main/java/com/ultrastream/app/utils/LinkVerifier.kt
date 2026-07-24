package com.ultrastream.app.utils

import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class LinkVerifier @Inject constructor(
    private val okHttpClient: OkHttpClient
) {

    suspend fun verifyLink(url: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .head()
                    .addHeader("User-Agent", "UltraStream/1.0 (Android)")
                    .addHeader("Referer", "https://ultrastream.app/")
                    .build()
                val response = okHttpClient.newCall(request).execute()
                val isValid = response.code in 200..299
                response.close()
                isValid
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun verifyM3ULinks(content: String): List<String> {
        // Extract all URLs from M3U and verify them, return list of working URLs
        // For simplicity, we'll just parse lines and check.
        val lines = content.lines()
        val workingLinks = mutableListOf<String>()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                if (verifyLink(trimmed)) {
                    workingLinks.add(trimmed)
                }
            }
        }
        return workingLinks
    }
}
