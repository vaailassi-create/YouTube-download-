package com.example.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.HEAD
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * Retrofit interface for streaming media downloads without buffering
 * entire files into memory.
 */
interface MediaDownloadApiService {

    @Streaming
    @GET
    suspend fun downloadMedia(
        @Url fileUrl: String,
        @Header("Range") rangeHeader: String? = null
    ): Response<ResponseBody>

    @HEAD
    suspend fun checkMediaHeaders(
        @Url fileUrl: String
    ): Response<Void>
}
