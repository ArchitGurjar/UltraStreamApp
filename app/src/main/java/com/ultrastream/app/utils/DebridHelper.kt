package com.ultrastream.app.utils

import com.ultrastream.app.network.AllDebridApi
import com.ultrastream.app.network.PremiumizeApi
import com.ultrastream.app.network.*
import com.ultrastream.app.network.RealDebridApi
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebridHelper @Inject constructor(
    private val realDebridApi: RealDebridApi,
    private val allDebridApi: AllDebridApi,
    private val premiumizeApi: PremiumizeApi
) {

    enum class DebridProvider {
        REAL_DEBRID, ALL_DEBRID, PREMIUMIZE
    }

    enum class DebridStatus {
        NOT_CONFIGURED, ACTIVE, EXPIRED, INVALID
    }

    suspend fun resolveStreamUrl(
        url: String,
        debridKey: String?,
        provider: DebridProvider = DebridProvider.REAL_DEBRID
    ): String {
        if (debridKey.isNullOrBlank()) return url

        return when (provider) {
            DebridProvider.REAL_DEBRID -> resolveRealDebrid(url, debridKey)
            DebridProvider.ALL_DEBRID -> resolveAllDebrid(url, debridKey)
            DebridProvider.PREMIUMIZE -> resolvePremiumize(url, debridKey)
        }
    }

    // =========================== REAL-DEBRID ===========================
    private suspend fun resolveRealDebrid(url: String, apiKey: String): String {
        val auth = "Bearer $apiKey"

        if (url.startsWith("magnet:")) {
            return resolveRealDebridMagnet(url, auth)
        }

        if (url.matches(Regex("^[a-fA-F0-9]{40}$"))) {
            val magnet = "magnet:?xt=urn:btih:$url"
            return resolveRealDebridMagnet(magnet, auth)
        }

        return applyDebridParams(url, apiKey)
    }

    private suspend fun resolveRealDebridMagnet(magnet: String, auth: String): String {
        val hash = extractHash(magnet)
        if (hash.isEmpty()) return magnet

        try {
            val availability = realDebridApi.checkInstantAvailability(auth, hash)
            if (availability.isNotEmpty()) {
                val cached = availability.values.firstOrNull { it.isNotEmpty() }
                if (cached != null) {
                    val addResponse = realDebridApi.addMagnet(auth, magnet)
                    val torrentId = addResponse.id
                    realDebridApi.selectFiles(auth, torrentId, "all")
                    var status = realDebridApi.getTorrentStatus(auth, torrentId)
                    var attempts = 0
                    while (status.status != "downloaded" && status.status != "ready" && attempts < 60) {
                        delay(1000)
                        status = realDebridApi.getTorrentStatus(auth, torrentId)
                        attempts++
                    }
                    if (status.status == "downloaded" || status.status == "ready") {
                        val link = status.links.firstOrNull()
                        if (link != null) {
                            val unrestricted = realDebridApi.unrestrictLink(auth, link)
                            return unrestricted.link
                        }
                    }
                }
            }
            return magnet
        } catch (e: Exception) {
            return magnet
        }
    }

    // =========================== ALL-DEBRID ===========================
    private suspend fun resolveAllDebrid(url: String, apiKey: String): String {
        if (url.startsWith("magnet:")) {
            return resolveAllDebridMagnet(url, apiKey)
        }
        if (url.matches(Regex("^[a-fA-F0-9]{40}$"))) {
            val magnet = "magnet:?xt=urn:btih:$url"
            return resolveAllDebridMagnet(magnet, apiKey)
        }
        return applyDebridParams(url, apiKey) // AllDebrid uses similar param? but we'll just return as-is for direct links
    }

    private suspend fun resolveAllDebridMagnet(magnet: String, apiKey: String): String {
        val hash = extractHash(magnet)
        if (hash.isEmpty()) return magnet

        try {
            // Step 1: Upload magnet
            val uploadResponse = allDebridApi.uploadMagnet(apiKey, magnet)
            if (!uploadResponse.status || uploadResponse.id == null) {
                return magnet
            }
            val torrentId = uploadResponse.id

            // Step 2: Wait for status to be "Completed"
            var statusResponse: AllDebridStatusResponse
            var attempts = 0
            do {
                delay(2000)
                statusResponse = allDebridApi.getMagnetStatus(apiKey, torrentId)
                attempts++
            } while (statusResponse.status != "Completed" && attempts < 60)

            if (statusResponse.status == "Completed") {
                // Step 3: Get link
                val linkResponse = allDebridApi.getMagnetLink(apiKey, torrentId)
                if (linkResponse.status && linkResponse.link != null) {
                    return linkResponse.link
                }
            }
            return magnet
        } catch (e: Exception) {
            return magnet
        }
    }

    // =========================== PREMIUMIZE ===========================
    private suspend fun resolvePremiumize(url: String, apiKey: String): String {
        if (url.startsWith("magnet:")) {
            return resolvePremiumizeMagnet(url, apiKey)
        }
        if (url.matches(Regex("^[a-fA-F0-9]{40}$"))) {
            val magnet = "magnet:?xt=urn:btih:$url"
            return resolvePremiumizeMagnet(magnet, apiKey)
        }
        return applyDebridParams(url, apiKey)
    }

    private suspend fun resolvePremiumizeMagnet(magnet: String, apiKey: String): String {
        val hash = extractHash(magnet)
        if (hash.isEmpty()) return magnet

        try {
            // Step 1: Create transfer
            val transferResponse = premiumizeApi.createTransfer(apiKey, magnet)
            if (!transferResponse.status || transferResponse.id == null) {
                return magnet
            }
            val transferId = transferResponse.id

            // Step 2: Wait for status to be "finished"
            var statusResponse: PremiumizeStatusResponse
            var attempts = 0
            do {
                delay(2000)
                statusResponse = premiumizeApi.getTransferStatus(apiKey, transferId)
                attempts++
            } while (statusResponse.status != "finished" && attempts < 60)

            if (statusResponse.status == "finished") {
                // Step 3: Get download link
                val itemResponse = premiumizeApi.getItemDetails(apiKey, transferId)
                if (itemResponse.status && itemResponse.content != null) {
                    // Return first link
                    val link = itemResponse.content.firstOrNull()?.link
                    if (link != null) {
                        return link
                    }
                }
            }
            return magnet
        } catch (e: Exception) {
            return magnet
        }
    }

    // =========================== HELPERS ===========================
    private fun extractHash(magnet: String): String {
        val match = Regex("btih:([a-fA-F0-9]{40})").find(magnet)
        return match?.groupValues?.get(1) ?: ""
    }

    fun applyDebridParams(url: String, debridKey: String): String {
        if (debridKey.isBlank()) return url
        val separator = if (url.contains("?")) "&" else "?"
        return "$url${separator}realdebrid=$debridKey"
    }

    fun getDebridStatus(key: String): DebridStatus {
        if (key.isBlank()) return DebridStatus.NOT_CONFIGURED
        if (key.length < 20) return DebridStatus.INVALID
        return DebridStatus.ACTIVE
    }
}
