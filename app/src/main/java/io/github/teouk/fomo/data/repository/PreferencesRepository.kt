package io.github.teouk.fomo.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.teouk.fomo.model.City
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** The user's settings. An interface, so ViewModel tests can use an in-memory fake. */
interface SettingsStore {
    val homeCity: Flow<City>
    val reminderLeadHours: Flow<Int>

    /** Calendar Clash Check switched on in Profile (off until the user chooses it). */
    val clashCheckEnabled: Flow<Boolean>

    suspend fun setHomeCity(city: City)

    suspend fun setReminderLeadHours(hours: Int)

    suspend fun setClashCheckEnabled(enabled: Boolean)
}

/**
 * Small user settings, stored with DataStore. Unlike SharedPreferences, DataStore never
 * reads or writes on the main thread and exposes changes as a [Flow].
 */
class PreferencesRepository(private val context: Context) : SettingsStore {
    override val homeCity: Flow<City> = context.dataStore.data.map { prefs ->
        prefs[HOME_CITY]?.let { name -> City.entries.firstOrNull { it.name == name } } ?: City.DEFAULT
    }

    override val reminderLeadHours: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[REMINDER_LEAD_HOURS] ?: DEFAULT_REMINDER_LEAD_HOURS
    }

    override val clashCheckEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[CLASH_CHECK_ENABLED] ?: false
    }

    override suspend fun setClashCheckEnabled(enabled: Boolean) {
        context.dataStore.edit { it[CLASH_CHECK_ENABLED] = enabled }
    }

    override suspend fun setHomeCity(city: City) {
        context.dataStore.edit { it[HOME_CITY] = city.name }
    }

    override suspend fun setReminderLeadHours(hours: Int) {
        context.dataStore.edit { it[REMINDER_LEAD_HOURS] = hours }
    }

    companion object {
        const val DEFAULT_REMINDER_LEAD_HOURS = 2
        val REMINDER_LEAD_OPTIONS = listOf(1, 2, 6, 24)
        private val HOME_CITY = stringPreferencesKey("home_city")
        private val REMINDER_LEAD_HOURS = intPreferencesKey("reminder_lead_hours")
        private val CLASH_CHECK_ENABLED = booleanPreferencesKey("clash_check_enabled")
    }
}
