package io.github.teouk.fomo.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.teouk.fomo.FomoApplication
import io.github.teouk.fomo.data.repository.Account
import io.github.teouk.fomo.data.calendar.CalendarSource
import io.github.teouk.fomo.data.repository.AuthRepository
import io.github.teouk.fomo.data.repository.PreferencesRepository
import io.github.teouk.fomo.data.repository.SettingsStore
import io.github.teouk.fomo.model.City
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val account: Account? = null,
    val homeCity: City = City.DEFAULT,
    val reminderLeadHours: Int = PreferencesRepository.DEFAULT_REMINDER_LEAD_HOURS,
    val clashCheckEnabled: Boolean = false,
)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val settings: SettingsStore,
    private val calendar: CalendarSource,
) : ViewModel() {
    val uiState: StateFlow<ProfileUiState> =
        combine(
            authRepository.account,
            settings.homeCity,
            settings.reminderLeadHours,
            settings.clashCheckEnabled,
        ) { account, city, hours, clashOn ->
            // Shown as on only if the permission is still granted (it can be revoked in Settings).
            ProfileUiState(account, city, hours, clashOn && calendar.hasPermission())
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            ProfileUiState(account = authRepository.currentAccount),
        )

    fun setDisplayName(name: String) {
        viewModelScope.launch { authRepository.updateDisplayName(name.trim()) }
    }

    fun setHomeCity(city: City) {
        viewModelScope.launch { settings.setHomeCity(city) }
    }

    fun setReminderLeadHours(hours: Int) {
        viewModelScope.launch { settings.setReminderLeadHours(hours) }
    }

    fun setClashCheckEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setClashCheckEnabled(enabled) }
    }

    fun signOut() = authRepository.signOut()

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as FomoApplication).container
                ProfileViewModel(container.authRepository, container.settings, container.calendarRepository)
            }
        }
    }
}
