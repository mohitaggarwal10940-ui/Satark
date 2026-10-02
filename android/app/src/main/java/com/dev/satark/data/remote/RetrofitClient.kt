package com.dev.satark.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    /**
     * Centralized Base URL Configuration:
     * - Android Emulator: "http://10.0.2.2:8000/"
     * - Physical Device via local Wi-Fi: "http://<YOUR_IP>:8000/"
     */
    const val EMULATOR_BASE_URL = "http://10.0.2.2:8000/"
    const val PHYSICAL_DEVICE_BASE_URL = "http://192.168.1.100:8000/"

    // Active Base URL - easily switchable
    var currentBaseUrl: String = EMULATOR_BASE_URL
        private set

    fun setBaseUrl(newUrl: String) {
        val sanitized = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
        currentBaseUrl = sanitized
        retrofitInstance = null
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Volatile
    private var retrofitInstance: Retrofit? = null

    private fun getRetrofit(): Retrofit {
        return retrofitInstance ?: synchronized(this) {
            retrofitInstance ?: Retrofit.Builder()
                .baseUrl(currentBaseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build().also { retrofitInstance = it }
        }
    }

    val apiService: ApiService
        get() = getRetrofit().create(ApiService::class.java)
}
