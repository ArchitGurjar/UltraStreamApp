package com.ultrastream.app.data.models

data class RecommendedAddon(
    val name: String,
    val description: String,
    val url: String,
    val isInstalled: Boolean = false
)
