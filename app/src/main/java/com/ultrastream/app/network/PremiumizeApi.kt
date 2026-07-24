package com.ultrastream.app.network

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface PremiumizeApi {

    /**
     * Create a new transfer for a magnet link.
     * Endpoint: transfer/create
     */
    @GET("transfer/create")
    suspend fun createTransfer(
        @Header("Authorization") auth: String,
        @Query("src") src: String
    ): PremiumizeTransferResponse

    /**
     * List all transfers and get status of a specific transfer.
     * Endpoint: transfer/list
     */
    @GET("transfer/list")
    suspend fun getTransferStatus(
        @Header("Authorization") auth: String,
        @Query("id") id: String
    ): PremiumizeStatusResponse

    /**
     * Get detailed information about a file or folder.
     * Endpoint: item/details
     */
    @GET("item/details")
    suspend fun getItemDetails(
        @Header("Authorization") auth: String,
        @Query("id") id: String
    ): PremiumizeItemResponse
}

// ============================================================
// CREATE TRANSFER RESPONSE
// ============================================================
data class PremiumizeTransferResponse(
    val status: String? = null,
    val id: String? = null,
    val name: String? = null,
    val message: String? = null
)

// ============================================================
// STATUS / LIST RESPONSE
// ============================================================
data class PremiumizeStatusResponse(
    val status: String? = null,
    val message: String? = null,
    val id: String? = null,
    val transfers: List<PremiumizeTransferItem>? = null
)

data class PremiumizeTransferItem(
    val id: String? = null,
    val name: String? = null,
    val status: String? = null,
    val message: String? = null,
    val progress: Double? = null,
    val file_id: String? = null,
    val folder_id: String? = null,
    val size: Long? = null,
    val created_at: String? = null,
    val finished_at: String? = null,
    val files: List<PremiumizeFileInfo>? = null
)

data class PremiumizeFileInfo(
    val id: String? = null,
    val name: String? = null,
    val size: Long? = null,
    val link: String? = null,
    val stream_link: String? = null,
    val type: String? = null,
    val path: String? = null
)

// ============================================================
// ITEM DETAILS RESPONSE
// ============================================================
data class PremiumizeItemResponse(
    val status: String? = null,
    val message: String? = null,
    val id: String? = null,
    val name: String? = null,
    val type: String? = null,
    val size: Long? = null,
    val link: String? = null,
    val stream_link: String? = null,
    val content: List<PremiumizeContent>? = null
)

data class PremiumizeContent(
    val id: String? = null,
    val name: String? = null,
    val size: Long? = null,
    val link: String? = null,
    val stream_link: String? = null,
    val type: String? = null,
    val path: String? = null
)
