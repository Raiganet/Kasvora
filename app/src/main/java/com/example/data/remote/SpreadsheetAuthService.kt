package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class SpreadsheetRequest(
    @Json(name = "action") val action: String,
    @Json(name = "email") val email: String,
    @Json(name = "name") val name: String? = null,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class UserResponseInfo(
    @Json(name = "email") val email: String,
    @Json(name = "name") val name: String
)

@JsonClass(generateAdapter = true)
data class SpreadsheetResponse(
    @Json(name = "status") val status: String,
    @Json(name = "message") val message: String,
    @Json(name = "user") val user: UserResponseInfo? = null
)

interface SpreadsheetAuthApi {
    @POST
    suspend fun authenticateUser(
        @Url url: String,
        @Body request: SpreadsheetRequest
    ): SpreadsheetResponse
}

object SpreadsheetAuthClient {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    val api: SpreadsheetAuthApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://script.google.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(SpreadsheetAuthApi::class.java)
    }
}
