package com.ultrastream.app.utils

import com.ultrastream.app.network.RealDebridApi
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebridHelper @Inject constructor(
    private val realDebridApi: RealDebridApi
) {

    suspend fun resolveStreamUrl(url: String, debridKey: String?): String {
        if (debridKey.isNullOrBlank()) return url

        val auth = "Bearer $debridKey"

        if (url.startsWith("magnet:")) {
            return resolveMagnet(url, auth)
        }

        if (url.matches(Regex("^[a-fA-F0-9]{40}$"))) {
            val magnet = "magnet:?xt=urn:btih:$url"
            return resolveMagnet(magnet, auth)
        }

        return applyDebridParams(url, debridKey)
    }

    private suspend fun resolveMagnet(magnet: String, auth: String): String {
        val hash = extractHash(magnet)
        if (hash.isEmpty()) return magnet

        val availability = realDebridApi.checkInstantAvailability(auth, hash)
        if (availability.isNotEmpty()) {
            val cached = availability.values.firstOrNull { it.isNotEmpty() }
            if (cached != null) {
                val addResponse = realDebridApi.addMagnet(auth, magnet)
                val torrentId = addResponse.id
                var status = realDebridApi.getTorrentStatus(auth, torrentId)
                var attempts = 0
                while (status.status != "downloaded" && status.status != "ready" && attempts < 30) {
                    delay(1000)
                    status = realDebridApi.getTorrentStatus(auth, torrentId)
                    attempts++
                }
                if (status.status == "downloaded" || status.status == "ready") {
                    val link = status.links.firstOrNull() ?: return magnet
                    val unrestricted = realDebridApi.unrestrictLink(auth, link)
                    return unrestricted.link
                }
            }
        }
        return magnet
    }

    private fun extractHash(magnet: String): String {
        val match = Regex("btih:([a-fA-F0-9]{40})").find(magnet)
        return match?.groupValues?.get(1) ?: ""
    }

    // FIXED: Made public so StreamRepository can access it without visibility errors
    fun applyDebridParams(url: String, debridKey: String): String {
        if (debridKey.isBlank()) return url
        val separator = if (url.contains("?")) "&" else "?"
        return "$url${separator}realdebrid=$debridKey"
    }
}
