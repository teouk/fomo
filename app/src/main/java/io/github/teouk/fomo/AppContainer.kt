package io.github.teouk.fomo

import android.content.Context
import io.github.teouk.fomo.data.local.FomoDatabase
import io.github.teouk.fomo.data.location.LocationRepository
import io.github.teouk.fomo.data.remote.EventDataSource
import io.github.teouk.fomo.data.remote.datathistle.DataThistleDataSource
import io.github.teouk.fomo.data.remote.ticketmaster.TicketmasterDataSource
import io.github.teouk.fomo.data.remote.ApiClients
import io.github.teouk.fomo.data.repository.AuthRepository
import io.github.teouk.fomo.data.repository.EventRepository
import io.github.teouk.fomo.data.repository.LibraryRepository
import io.github.teouk.fomo.data.repository.PreferencesRepository
import io.github.teouk.fomo.data.repository.SettingsStore
import io.github.teouk.fomo.data.repository.WeatherRepository
import io.github.teouk.fomo.data.sample.SampleTicketmasterService
import io.github.teouk.fomo.work.ReminderScheduler

/**
 * Creates the app's long-lived objects once and hands them to whoever needs them.
 * This is "manual dependency injection": instead of each screen building its own
 * Retrofit client or database, they all ask the container. Hilt automates the same idea.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    /**
     * Which events API to use, decided by which secret is in local.properties:
     * Data Thistle (main source), else Ticketmaster, else the offline sample events
     * (a safety net for building without any key; not used for the submission).
     */
    private val eventDataSource: EventDataSource = when {
        BuildConfig.DATATHISTLE_TOKEN.isNotBlank() ->
            DataThistleDataSource(ApiClients.createDataThistleService(BuildConfig.DATATHISTLE_TOKEN, BuildConfig.DEBUG))
        BuildConfig.TICKETMASTER_API_KEY.isNotBlank() ->
            TicketmasterDataSource(ApiClients.createTicketmasterService(BuildConfig.TICKETMASTER_API_KEY, BuildConfig.DEBUG))
        else -> TicketmasterDataSource(SampleTicketmasterService(), providerName = SAMPLE_PROVIDER_NAME)
    }

    /** True when no API secret is set, so the app shows the built-in sample events. */
    val usesSampleData: Boolean = eventDataSource.providerName == SAMPLE_PROVIDER_NAME

    val eventRepository = EventRepository(eventDataSource, hasApiKey = true)

    private val database: FomoDatabase = FomoDatabase.create(appContext)

    val libraryRepository = LibraryRepository(database.savedEventDao())

    val settings: SettingsStore = PreferencesRepository(appContext)

    val weatherRepository = WeatherRepository(ApiClients.createOpenMeteoService())

    val reminderScheduler = ReminderScheduler(appContext)

    val authRepository = AuthRepository()

    val locationRepository = LocationRepository(appContext)

    private companion object {
        const val SAMPLE_PROVIDER_NAME = "Sample data"
    }
}
