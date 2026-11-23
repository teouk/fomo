package io.github.teouk.fomo

import android.content.Context
import io.github.teouk.fomo.data.local.FomoDatabase
import io.github.teouk.fomo.data.remote.TicketmasterApi
import io.github.teouk.fomo.data.repository.EventRepository
import io.github.teouk.fomo.data.repository.LibraryRepository
import io.github.teouk.fomo.data.repository.PreferencesRepository
import io.github.teouk.fomo.data.repository.SettingsStore
import io.github.teouk.fomo.data.sample.SampleTicketmasterService

/**
 * Creates the app's long-lived objects once and hands them to whoever needs them.
 * This is "manual dependency injection": instead of each screen building its own
 * Retrofit client or database, they all ask the container. Hilt automates the same idea.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    /** True when no Ticketmaster key is set, so the app shows built-in sample events instead. */
    val usesSampleData: Boolean = BuildConfig.TICKETMASTER_API_KEY.isBlank()

    val eventRepository: EventRepository = EventRepository(
        service = if (usesSampleData) {
            SampleTicketmasterService()
        } else {
            TicketmasterApi.createService(BuildConfig.TICKETMASTER_API_KEY, logRequests = BuildConfig.DEBUG)
        },
        hasApiKey = true,
    )

    private val database: FomoDatabase = FomoDatabase.create(appContext)

    val libraryRepository = LibraryRepository(database.savedEventDao())

    val settings: SettingsStore = PreferencesRepository(appContext)
}
