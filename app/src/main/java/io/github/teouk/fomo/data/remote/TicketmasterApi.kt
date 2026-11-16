package io.github.teouk.fomo.data.remote

import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Builds the Retrofit client for Ticketmaster. Created once, in AppContainer. */
object TicketmasterApi {
    private const val BASE_URL = "https://app.ticketmaster.com/"
    private const val API_KEY_PARAMETER = "apikey"

    /** Ticketmaster adds fields over time; unknown ones are skipped instead of crashing. */
    val json = Json { ignoreUnknownKeys = true }

    fun createService(apiKey: String, logRequests: Boolean): TicketmasterService {
        // Adds ?apikey=... to every request in one place, so no caller handles the key.
        val apiKeyInterceptor = Interceptor { chain ->
            val url = chain.request().url.newBuilder()
                .addQueryParameter(API_KEY_PARAMETER, apiKey)
                .build()
            chain.proceed(chain.request().newBuilder().url(url).build())
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(apiKeyInterceptor)
            .apply {
                // Request logs are for development only. They are off in release builds
                // because they would print the API key into Logcat.
                if (logRequests) {
                    addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                }
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TicketmasterService::class.java)
    }
}
