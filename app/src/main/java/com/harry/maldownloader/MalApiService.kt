package com.harry.maldownloader

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// Retrofit service to interact with MAL API
interface MalApiService {

    @GET("anime/{id}")
    suspend fun getAnimeDetails(@Path("id") id: String, @Query("fields") fields: String = "main_picture,title"):
            MalApiResponse
}

// Retrofit response data class
// Model as per your expected data - simplify or expand as needed
data class MalApiResponse(
    val main_picture: ImageData?,
    val title: String
)

data class ImageData(
    val medium: String?,
    val large: String?
)

// Create Retrofit instance and service
fun provideMalApiService(): MalApiService {
    val client = OkHttpClient.Builder()
        .build()

    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.myanimelist.net/v2/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .build()

    return retrofit.create(MalApiService::class.java)
}
