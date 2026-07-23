package com.ultrastream.app.utils

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebridHelper @Inject constructor() {

    fun applyDebrid(url: String, debridKey: String?): String {
        if (debridKey.isNullOrBlank()) return url
        // Check if addon supports debrid (torrentio, orion, etc.)
        val lower = url.lowercase()
        if (lower.contains("torrentio") || lower.contains("orionoid")) {
            val separator = if (url.contains("?")) "&" else "?"
            return "$url${separator}realdebrid=$debridKey"
        }
        return url
    }
}
