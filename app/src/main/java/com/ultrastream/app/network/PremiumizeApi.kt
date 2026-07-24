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
