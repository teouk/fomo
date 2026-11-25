package io.github.teouk.fomo

import io.github.teouk.fomo.data.repository.SettingsStore
import io.github.teouk.fomo.model.City
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory settings for tests, instead of DataStore (which needs an Android Context). */
class FakeSettings : SettingsStore {
    override val homeCity = MutableStateFlow(City.SWANSEA)
    override val reminderLeadHours = MutableStateFlow(2)
    override val clashCheckEnabled = MutableStateFlow(false)

    override suspend fun setHomeCity(city: City) {
        homeCity.value = city
    }

    override suspend fun setClashCheckEnabled(enabled: Boolean) {
        clashCheckEnabled.value = enabled
    }

    override suspend fun setReminderLeadHours(hours: Int) {
        reminderLeadHours.value = hours
    }
}
