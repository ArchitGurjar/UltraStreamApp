package com.ultrastream.app.utils

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class LinkVerifier @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun verifyLinkStatus(url: String?): Boolean {
        if (url.isNullOrBlank() || url.startsWith("magnet:")) return false
        return withContext(Dispatchers.IO) {
            try {
                val headRequest = Request.Builder()
                    .url(url)
                    .head()
                    .addHeader("User-Agent", "UltraStream/1.0 (Android)")
                    .build()
                val headResponse = client.newCall(headRequest).execute()
                if (headResponse.isSuccessful) {
                    headResponse.close()
                    return@withContext true
                }
                headResponse.close()

                val rangeRequest = Request.Builder()
                    .url(url)
                    .addHeader("Range", "bytes=0-0")
                    .addHeader("User-Agent", "UltraStream/1.0 (Android)")
                    .build()
                val rangeResponse = client.newCall(rangeRequest).execute()
                val success = rangeResponse.isSuccessful && (rangeResponse.code == 200 || rangeResponse.code == 206)
                rangeResponse.close()
                return@withContext success
            } catch (e: Exception) {
                false
            }
        }
    }
}
