package com.harry.maldownloader.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Provides Retrofit clients for both official MAL API and Jikan API with a
 * robust fallback strategy.
 */
object ApiClients {
    private const val MAL_BASE_URL = "https://api.myanimelist.net/v2/"
    private const val JIKAN_BASE_URL = "https://api.jikan.moe/v4/"

    /** Interceptor that injects X-MAL-CLIENT-ID on every request */
    private class MalClientIdInterceptor(private val clientIdProvider: () -> String) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val req = chain.request().newBuilder()
                .addHeader("X-MAL-CLIENT-ID", clientIdProvider())
                .build()
            return chain.proceed(req)
        }
    }

    fun malRetrofit(clientIdProvider: () -> String): Retrofit {
        val client = OkHttpClient.Builder()
            .addInterceptor(MalClientIdInterceptor(clientIdProvider))
            .build()
        return Retrofit.Builder()
            .baseUrl(MAL_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }

    fun jikanRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl(JIKAN_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}
