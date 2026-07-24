package com.ultrastream.app.network

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface PremiumizeApi {
    @GET("transfer/create")
    suspend fun createTransfer(
        @Header("Authorization") auth: String,
        @Query("src") src: String
    ): PremiumizeTransferResponse

    @GET("transfer/status")
    suspend fun getTransferStatus(
        @Header("Authorization") auth: String,
        @Query("id") id: String
    ): PremiumizeStatusResponse

    @GET("item/details")
    suspend fun getItemDetails(
        @Header("Authorization") auth: String,
        @Query("id") id: String
    ): PremiumizeItemResponse
}

data class PremiumizeTransferResponse(val status: Boolean, val id: String? = null, val message: String? = null)
data class PremiumizeStatusResponse(val status: String, val id: String? = null, val message: String? = null)
data class PremiumizeItemResponse(val status: Boolean, val content: List<PremiumizeContent>? = null, val message: String? = null)
data class PremiumizeContent(val link: String, val name: String)
