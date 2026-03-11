package com.cyberarcenal.huddle.network

import com.cyberarcenal.huddle.api.apis.V1Api
import com.cyberarcenal.huddle.api.infrastructure.ApiClient
import com.cyberarcenal.huddle.api.infrastructure.Serializer
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import android.os.Build

object ApiService {
    private val BASE_URL: String by lazy {
        if (isEmulator()) {
            "http://10.0.2.2:8000/"
        } else {
            "http://127.0.0.1:8000/"
        }
    }

    private fun isEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator")
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val token = TokenManager.accessToken
            val requestBuilder = original.newBuilder()

            if (!token.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }

            chain.proceed(requestBuilder.build())
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val apiClient = ApiClient(
        baseUrl = BASE_URL,
        okHttpClientBuilder = okHttpClient.newBuilder(),
        authNames = arrayOf()
    )

    // E-expose ang Retrofit instance gamit ang Serializer.gson para sa tamang parsing (OffsetDateTime, etc.)
    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(Serializer.gson))
            .build()
    }

    val v1Api: V1Api = apiClient.createService(V1Api::class.java)
}
