package com.ultrastream.app.utils

import com.ultrastream.app.network.AllDebridApi
import com.ultrastream.app.network.PremiumizeApi
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
        return applyDebridParams(url, apiKey)
    }

    private suspend fun resolveAllDebridMagnet(magnet: String, apiKey: String): String {
        val hash = extractHash(magnet)
        if (hash.isEmpty()) return magnet

        try {
            val uploadResponse = allDebridApi.uploadMagnet(apiKey, magnet)
            if (uploadResponse.status != true || uploadResponse.data?.id == null) {
                return magnet
            }
            val torrentId = uploadResponse.data.id

            var statusResponse = allDebridApi.getMagnetStatus(apiKey, torrentId)
            var attempts = 0
            while (statusResponse.data?.magnets?.firstOrNull()?.status != "Completed" && attempts < 60) {
                delay(2000)
                statusResponse = allDebridApi.getMagnetStatus(apiKey, torrentId)
                attempts++
            }
            val magnetItem = statusResponse.data?.magnets?.firstOrNull()
            if (magnetItem?.status == "Completed") {
                val linkResponse = allDebridApi.getMagnetLink(apiKey, torrentId)
                if (linkResponse.status == true && linkResponse.data?.link != null) {
                    return linkResponse.data.link
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
            val transferResponse = premiumizeApi.createTransfer(apiKey, magnet)
            if (transferResponse.status != "success" || transferResponse.id == null) {
                return magnet
            }
            val transferId = transferResponse.id

            var statusResponse = premiumizeApi.getTransferStatus(apiKey, transferId)
            var attempts = 0
            while (statusResponse.transfers?.firstOrNull()?.status != "finished" && attempts < 60) {
                delay(2000)
                statusResponse = premiumizeApi.getTransferStatus(apiKey, transferId)
                attempts++
            }
            val transferItem = statusResponse.transfers?.firstOrNull()
            if (transferItem?.status == "finished") {
                val itemResponse = premiumizeApi.getItemDetails(apiKey, transferId)
                if (itemResponse.status == "success" && itemResponse.link != null) {
                    return itemResponse.link
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
