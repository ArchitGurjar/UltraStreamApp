package com.ultrastream.app.network

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface AllDebridApi {

    /**
     * Upload a magnet link to AllDebrid.
     * Endpoint: magnet/upload
     */
    @GET("magnet/upload")
    suspend fun uploadMagnet(
        @Header("Authorization") auth: String,
        @Query("magnet") magnet: String
    ): AllDebridUploadResponse

    /**
     * Get the status of a magnet transfer.
     * Endpoint: magnet/status
     */
    @GET("magnet/status")
    suspend fun getMagnetStatus(
        @Header("Authorization") auth: String,
        @Query("id") id: String
    ): AllDebridStatusResponse

    /**
     * Get a direct download link for a completed magnet.
     * Endpoint: magnet/link
     */
    @GET("magnet/link")
    suspend fun getMagnetLink(
        @Header("Authorization") auth: String,
        @Query("id") id: String
    ): AllDebridLinkResponse
}

// ============================================================
// UPLOAD RESPONSE
// ============================================================
data class AllDebridUploadResponse(
    val status: Boolean? = null,
    val message: String? = null,
    val data: AllDebridUploadData? = null
)

data class AllDebridUploadData(
    val id: String? = null,
    val name: String? = null,
    val size: Long? = null,
    val hash: String? = null,
    val links: List<String>? = null
)

// ============================================================
// STATUS RESPONSE
// ============================================================
data class AllDebridStatusResponse(
    val status: Boolean? = null,
    val message: String? = null,
    val data: AllDebridStatusData? = null
)

data class AllDebridStatusData(
    val id: String? = null,
    val magnets: List<AllDebridMagnetItem>? = null
)

data class AllDebridMagnetItem(
    val id: String? = null,
    val filename: String? = null,
    val size: Long? = null,
    val status: String? = null,
    val statusCode: Int? = null,
    val downloaded: Long? = null,
    val uploaded: Long? = null,
    val speed: Long? = null,
    val seeders: Int? = null,
    val files: List<AllDebridFileLink>? = null
)

data class AllDebridFileLink(
    val link: String? = null,
    val filename: String? = null,
    val size: Long? = null,
    val stream: List<AllDebridStreamInfo>? = null
)

data class AllDebridStreamInfo(
    val url: String? = null,
    val quality: String? = null,
    val language: String? = null,
    val subtitles: List<AllDebridSubtitle>? = null
)

data class AllDebridSubtitle(
    val url: String? = null,
    val lang: String? = null
)

// ============================================================
// LINK RESPONSE
// ============================================================
data class AllDebridLinkResponse(
    val status: Boolean? = null,
    val message: String? = null,
    val data: AllDebridLinkData? = null
)

data class AllDebridLinkData(
    val link: String? = null,
    val filename: String? = null,
    val filesize: Long? = null,
    val host: String? = null,
    val hostDomain: String? = null,
    val stream: List<AllDebridStreamInfo>? = null
)
