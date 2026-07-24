package com.ultrastream.app.utils

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LinkVerifier @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun verifyLinkStatus(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        // Use HEAD request with fallback to GET Range
        try {
            val headRequest = Request.Builder()
                .url(url)
                .head()
                .build()
            val headResponse = client.newCall(headRequest).execute()
            if (headResponse.isSuccessful) {
                headResponse.close()
                return true
            }
            // If HEAD fails (e.g., 405), try GET with Range: bytes=0-0
            if (headResponse.code == 405) {
                val rangeRequest = Request.Builder()
                    .url(url)
                    .addHeader("Range", "bytes=0-0")
                    .build()
                val rangeResponse = client.newCall(rangeRequest).execute()
                val success = rangeResponse.isSuccessful || rangeResponse.code == 206
                rangeResponse.close()
                return success
            }
            headResponse.close()
            return false
        } catch (e: Exception) {
            return false
        }
    }
}
