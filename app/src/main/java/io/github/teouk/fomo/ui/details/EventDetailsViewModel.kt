package io.github.teouk.fomo.ui.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.teouk.fomo.FomoApplication
import io.github.teouk.fomo.data.calendar.Clash
import io.github.teouk.fomo.data.repository.ClashRepository
import io.github.teouk.fomo.data.repository.DataError
import io.github.teouk.fomo.data.repository.EventRepository
import io.github.teouk.fomo.data.repository.EventStatus
import io.github.teouk.fomo.data.repository.LibraryRepository
import io.github.teouk.fomo.data.repository.Outcome
import io.github.teouk.fomo.data.repository.SettingsStore
import io.github.teouk.fomo.data.repository.WeatherRepository
import io.github.teouk.fomo.model.Event
import io.github.teouk.fomo.model.WeatherForecast
import io.github.teouk.fomo.work.ReminderScheduler
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EventDetailsUiState(
    val isLoading: Boolean = true,
    val event: Event? = null,
    val status: EventStatus = EventStatus(),
    val weather: WeatherForecast? = null,
    /** Calendar Clash Check: the overlapping calendar entry, if any. */
    val clash: Clash? = null,
    val error: DataError? = null,
    val providerName: String = "",
)

/** One-off things the screen should do once, such as showing a Snackbar. */
sealed interface DetailsMessage {
    data class PlanAdded(val event: Event) : DetailsMessage

    data object PlanRemoved : DetailsMessage

    data class SaveToggled(val isSaved: Boolean) : DetailsMessage
}

class EventDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    private val eventRepository: EventRepository,
    private val libraryRepository: LibraryRepository,
    private val weatherRepository: WeatherRepository,
    private val reminderScheduler: ReminderScheduler,
    private val settings: SettingsStore,
    private val clashRepository: ClashRepository,
) : ViewModel() {

    /** The Activity's Intent extras arrive in SavedStateHandle automatically. */
    private val eventId: String = checkNotNull(savedStateHandle[EventDetailsActivity.EXTRA_EVENT_ID])

    private val loaded = MutableStateFlow(EventDetailsUiState(providerName = eventRepository.providerName))

    val uiState: StateFlow<EventDetailsUiState> =
        combine(loaded, libraryRepository.statuses) { state, statuses ->
            state.copy(status = statuses[eventId] ?: EventStatus())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), EventDetailsUiState())

    /**
     * A Channel delivers each message exactly once, unlike state, which is replayed
     * after rotation. A Snackbar should not reappear just because the phone turned.
     */
    private val messageChannel = Channel<DetailsMessage>(Channel.BUFFERED)
    val messages: Flow<DetailsMessage> = messageChannel.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            loaded.update { it.copy(isLoading = true, error = null) }
            when (val outcome = eventRepository.getEvent(eventId)) {
                is Outcome.Success -> showEvent(outcome.data)
                is Outcome.Failure -> {
                    // Offline but saved or planned earlier? Show the stored copy instead.
                    val stored = libraryRepository.getStored(eventId)
                    if (stored != null) {
                        showEvent(stored)
                    } else {
                        loaded.update { it.copy(isLoading = false, error = outcome.error) }
                    }
                }
            }
        }
    }

    private suspend fun showEvent(event: Event) {
        loaded.update { it.copy(isLoading = false, event = event, error = null) }
        val clash = clashRepository.clashesFor(listOf(event))[event.id]
        loaded.update { it.copy(clash = clash) }
        val lat = event.venue?.latitude
        val lon = event.venue?.longitude
        val startsAt = event.startsAt
        if (lat != null && lon != null && startsAt != null) {
            val forecast = weatherRepository.forecastFor(lat, lon, startsAt)
            loaded.update { it.copy(weather = forecast) }
        }
    }

    fun toggleSave() {
        val event = loaded.value.event ?: return
        viewModelScope.launch {
            val nowSaved = libraryRepository.toggleSaved(event)
            messageChannel.send(DetailsMessage.SaveToggled(nowSaved))
        }
    }

    fun togglePlan() {
        val event = loaded.value.event ?: return
        viewModelScope.launch {
            if (uiState.value.status.isPlanned) {
                libraryRepository.setPlanned(event, planned = false)
                reminderScheduler.cancel(event.id)
                messageChannel.send(DetailsMessage.PlanRemoved)
            } else {
                libraryRepository.setPlanned(event, planned = true)
                reminderScheduler.schedule(event, settings.reminderLeadHours.first())
                messageChannel.send(DetailsMessage.PlanAdded(event))
            }
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as FomoApplication).container
                EventDetailsViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    eventRepository = container.eventRepository,
                    libraryRepository = container.libraryRepository,
                    weatherRepository = container.weatherRepository,
                    reminderScheduler = container.reminderScheduler,
                    settings = container.settings,
                    clashRepository = container.clashRepository,
                )
            }
        }
    }
}
