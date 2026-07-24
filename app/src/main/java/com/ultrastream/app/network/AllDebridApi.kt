package com.ultrastream.app.network

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface AllDebridApi {
    @GET("magnet/upload")
    suspend fun uploadMagnet(
        @Header("Authorization") auth: String,
        @Query("magnet") magnet: String
    ): AllDebridUploadResponse

    @GET("magnet/status")
    suspend fun getMagnetStatus(
        @Header("Authorization") auth: String,
        @Query("id") id: String
    ): AllDebridStatusResponse

    @GET("magnet/link")
    suspend fun getMagnetLink(
        @Header("Authorization") auth: String,
        @Query("id") id: String
    ): AllDebridLinkResponse
}

data class AllDebridUploadResponse(val status: Boolean, val id: String? = null, val message: String? = null)
data class AllDebridStatusResponse(val status: String, val id: String? = null, val link: String? = null)
data class AllDebridLinkResponse(val status: Boolean, val link: String? = null, val message: String? = null)
