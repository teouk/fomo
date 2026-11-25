package io.github.teouk.fomo.data.remote

import io.github.teouk.fomo.data.remote.datathistle.DataThistleService
import io.github.teouk.fomo.data.remote.weather.OpenMeteoService
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Builds the Retrofit clients for the app's two external APIs. Created once, in AppContainer. */
object ApiClients {
    /** APIs add fields over time; unknown ones are skipped instead of crashing the parse. */
    val json = Json { ignoreUnknownKeys = true }

    private val jsonConverter = json.asConverterFactory("application/json".toMediaType())

    /**
     * @param accessToken the Data Thistle token from local.properties (via BuildConfig).
     * @param logRequests log each request in Logcat; only for debug builds.
     */
    fun createDataThistleService(accessToken: String, logRequests: Boolean): DataThistleService {
        // Data Thistle wants the token in a header, not in the URL. Adding it in one
        // interceptor means no other code ever handles the token, and keeping it out of
        // URLs keeps it out of server logs.
        val authInterceptor = Interceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("Authorization", "Bearer $accessToken").build())
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .apply {
                // BASIC logs the method, URL and status code, not the headers, so the
                // token never appears in Logcat.
                if (logRequests) addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            }
            .build()
        return Retrofit.Builder()
            .baseUrl(DataThistleService.BASE_URL)
            .client(client)
            .addConverterFactory(jsonConverter)
            .build()
            .create(DataThistleService::class.java)
    }

    /** Open-Meteo needs no key, so a plain client is enough. */
    fun createOpenMeteoService(): OpenMeteoService =
        Retrofit.Builder()
            .baseUrl(OpenMeteoService.BASE_URL)
            .client(OkHttpClient())
            .addConverterFactory(jsonConverter)
            .build()
            .create(OpenMeteoService::class.java)
}
