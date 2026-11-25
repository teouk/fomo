package io.github.teouk.fomo.data.remote

import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import io.github.teouk.fomo.data.remote.datathistle.DataThistleService
import io.github.teouk.fomo.data.remote.ticketmaster.TicketmasterService
import io.github.teouk.fomo.data.remote.weather.OpenMeteoService
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Builds the Retrofit clients. Created once, in AppContainer. */
object ApiClients {
    private const val BASE_URL = "https://app.ticketmaster.com/"
    private const val API_KEY_PARAMETER = "apikey"

    /** Ticketmaster adds fields over time; unknown ones are skipped instead of crashing. */
    val json = Json { ignoreUnknownKeys = true }

    fun createTicketmasterService(apiKey: String, logRequests: Boolean): TicketmasterService {
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

    fun createDataThistleService(accessToken: String, logRequests: Boolean): DataThistleService {
        // Data Thistle wants the token in a header, not in the URL. Keeping it out of
        // URLs also keeps it out of server logs and browser histories.
        val authInterceptor = Interceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("Authorization", "Bearer $accessToken").build())
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .apply {
                if (logRequests) addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            }
            .build()
        return Retrofit.Builder()
            .baseUrl(DataThistleService.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(DataThistleService::class.java)
    }

    fun createOpenMeteoService(): OpenMeteoService =
        Retrofit.Builder()
            .baseUrl(OpenMeteoService.BASE_URL)
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenMeteoService::class.java)
}
