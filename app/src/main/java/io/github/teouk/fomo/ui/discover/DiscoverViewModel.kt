package io.github.teouk.fomo.ui.discover

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
import io.github.teouk.fomo.model.Category
import io.github.teouk.fomo.model.City
import io.github.teouk.fomo.model.DateWindow
import io.github.teouk.fomo.model.Event
import io.github.teouk.fomo.model.EventFilter
import io.github.teouk.fomo.ui.common.EventListItem
import io.github.teouk.fomo.util.Distance
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime

/** Everything the Discover screen shows. The Fragment only draws this; it decides nothing. */
data class DiscoverUiState(
    val city: City = City.DEFAULT,
    val category: Category? = null,
    val isLoading: Boolean = true,
    val thisWeekend: List<EventListItem> = emptyList(),
    val nearYou: List<EventListItem> = emptyList(),
    val error: DataError? = null,
    /** Who supplied the events, credited on screen (e.g. "Data Thistle"). */
    val providerName: String = "",
) {
    val isEmpty: Boolean get() = !isLoading && error == null && thisWeekend.isEmpty() && nearYou.isEmpty()
}

/**
 * Loads "This weekend" and "Near you" for the chosen city and category.
 *
 * Data flows one way: the Fragment sends user actions in (selectCategory, toggleSave…),
 * the ViewModel updates [uiState], and the Fragment redraws from it. The ViewModel
 * survives rotation, so nothing is reloaded when the phone turns.
 */
class DiscoverViewModel(
    private val eventRepository: EventRepository,
    private val libraryRepository: LibraryRepository,
    private val settings: SettingsStore,
    private val clashRepository: ClashRepository,
    private val savedStateHandle: SavedStateHandle,
    private val clock: Clock = Clock.systemUTC(),
) : ViewModel() {

    /** Kept in SavedStateHandle, so the chosen chip survives even if Android kills the app. */
    private val selectedCategory: StateFlow<Category?> = savedStateHandle.getStateFlow(KEY_CATEGORY, null)

    private val refreshRequests = MutableStateFlow(0)

    private data class Loaded(
        val city: City = City.DEFAULT,
        val isLoading: Boolean = true,
        val weekend: List<Event> = emptyList(),
        val upcoming: List<Event> = emptyList(),
        val weekendClashes: Map<String, Clash> = emptyMap(),
        val upcomingClashes: Map<String, Clash> = emptyMap(),
        val error: DataError? = null,
    )

    private val loaded = MutableStateFlow(Loaded())

    val uiState: StateFlow<DiscoverUiState> =
        combine(loaded, selectedCategory, libraryRepository.statuses) { data, category, statuses ->
            DiscoverUiState(
                city = data.city,
                category = category,
                isLoading = data.isLoading,
                thisWeekend = data.weekend.map { it.toItem(data.city, statuses, data.weekendClashes) },
                nearYou = data.upcoming.map { it.toItem(data.city, statuses, data.upcomingClashes) },
                error = data.error,
                providerName = eventRepository.providerName,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DiscoverUiState())

    init {
        viewModelScope.launch {
            // collectLatest cancels a load that is still running when the user picks
            // another chip, so an old, slower response can never overwrite a newer one.
            // The clash setting is included so switching it on in Profile updates the labels.
            combine(settings.homeCity, selectedCategory, refreshRequests, settings.clashCheckEnabled) { city, category, _, _ ->
                city to category
            }
                .collectLatest { (city, category) -> load(city, category) }
        }
    }

    fun selectCategory(category: Category?) {
        savedStateHandle[KEY_CATEGORY] = category
    }

    fun selectCity(city: City) {
        viewModelScope.launch { settings.setHomeCity(city) }
    }

    fun retry() {
        refreshRequests.update { it + 1 }
    }

    fun toggleSave(event: Event) {
        viewModelScope.launch { libraryRepository.toggleSaved(event) }
    }

    private suspend fun load(city: City, category: Category?) {
        loaded.update { it.copy(city = city, isLoading = true, error = null) }
        val filter = EventFilter(city.latitude, city.longitude, categories = setOfNotNull(category))
        val now = ZonedDateTime.now(clock).withZoneSameInstant(UK_TIME_ZONE)

        // Both requests run at the same time; coroutineScope waits for both.
        val (weekend, upcoming) = coroutineScope {
            val weekend = async { eventRepository.searchEvents(filter.copy(dateWindow = DateWindow.weekend(now))) }
            val upcoming = async { eventRepository.searchEvents(filter) }
            weekend.await() to upcoming.await()
        }

        loaded.value = when {
            upcoming is Outcome.Failure -> Loaded(city, isLoading = false, error = upcoming.error)
            weekend is Outcome.Failure -> Loaded(city, isLoading = false, error = weekend.error)
            else -> {
                val weekendEvents = (weekend as Outcome.Success).data
                val upcomingEvents = (upcoming as Outcome.Success).data
                Loaded(
                    city = city,
                    isLoading = false,
                    weekend = weekendEvents,
                    upcoming = upcomingEvents,
                    // Checked per list, not merged: the same event can appear in both with
                    // different times (its Saturday showing for "This weekend", tonight's for
                    // "Near you"), so one answer per event ID would be wrong for one of them.
                    weekendClashes = clashRepository.clashesFor(weekendEvents),
                    upcomingClashes = clashRepository.clashesFor(upcomingEvents),
                )
            }
        }
    }

    private fun Event.toItem(city: City, statuses: Map<String, EventStatus>, clashes: Map<String, Clash>): EventListItem {
        val lat = venue?.latitude
        val lon = venue?.longitude
        return EventListItem(
            event = this,
            distanceMiles = if (lat != null && lon != null) {
                Distance.miles(city.latitude, city.longitude, lat, lon)
            } else {
                distanceMiles
            },
            isSaved = statuses[id]?.isSaved == true,
            clash = clashes[id],
        )
    }

    companion object {
        private const val KEY_CATEGORY = "category"
        private const val STOP_TIMEOUT_MS = 5_000L
        val UK_TIME_ZONE: ZoneId = ZoneId.of("Europe/London")

        /** Tells Android how to build this ViewModel with its dependencies from AppContainer. */
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as FomoApplication).container
                DiscoverViewModel(
                    eventRepository = container.eventRepository,
                    libraryRepository = container.libraryRepository,
                    settings = container.settings,
                    clashRepository = container.clashRepository,
                    savedStateHandle = createSavedStateHandle(),
                )
            }
        }
    }
}
