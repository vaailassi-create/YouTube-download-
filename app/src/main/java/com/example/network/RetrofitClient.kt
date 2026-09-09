package com.example.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Singleton Retrofit provider configured specifically for large streaming downloads
 * from self-hosted and CC media servers.
 */
object RetrofitClient {

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }

        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .addInterceptor(logging)
            .build()
    }

    val downloadApi: MediaDownloadApiService by lazy {
        Retrofit.Builder()
            // Placeholder base URL required by Retrofit; dynamic full @Url is supplied in queries
            .baseUrl("https://localhost/")
            .client(okHttpClient)
            .build()
            .create(MediaDownloadApiService::class.java)
    }
}
