package io.github.teouk.fomo

import android.content.Context
import io.github.teouk.fomo.data.local.FomoDatabase
import io.github.teouk.fomo.data.location.LocationRepository
import io.github.teouk.fomo.data.ocr.MlKitPosterReader
import io.github.teouk.fomo.data.ocr.PosterReader
import io.github.teouk.fomo.data.remote.EventDataSource
import io.github.teouk.fomo.data.remote.datathistle.DataThistleDataSource
import io.github.teouk.fomo.data.remote.ApiClients
import io.github.teouk.fomo.data.repository.AuthRepository
import io.github.teouk.fomo.data.repository.EventRepository
import io.github.teouk.fomo.data.repository.LibraryRepository
import io.github.teouk.fomo.data.repository.PreferencesRepository
import io.github.teouk.fomo.data.repository.SettingsStore
import io.github.teouk.fomo.data.repository.WeatherRepository
import io.github.teouk.fomo.work.ReminderScheduler

/**
 * Creates the app's long-lived objects once and hands them to whoever needs them.
 * This is "manual dependency injection": instead of each screen building its own
 * Retrofit client or database, they all ask the container. Hilt automates the same idea.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    /** Events come from the Data Thistle API, with the token from local.properties. */
    private val eventDataSource: EventDataSource =
        DataThistleDataSource(ApiClients.createDataThistleService(BuildConfig.DATATHISTLE_TOKEN, BuildConfig.DEBUG))

    /** Without a token, every request would be rejected, so the repository reports it straight away. */
    val eventRepository = EventRepository(eventDataSource, hasApiKey = BuildConfig.DATATHISTLE_TOKEN.isNotBlank())

    private val database: FomoDatabase = FomoDatabase.create(appContext)

    val libraryRepository = LibraryRepository(database.savedEventDao())

    val settings: SettingsStore = PreferencesRepository(appContext)

    val weatherRepository = WeatherRepository(ApiClients.createOpenMeteoService())

    val reminderScheduler = ReminderScheduler(appContext)

    val authRepository = AuthRepository()

    val locationRepository = LocationRepository(appContext)

    val posterReader: PosterReader = MlKitPosterReader(appContext)

}
