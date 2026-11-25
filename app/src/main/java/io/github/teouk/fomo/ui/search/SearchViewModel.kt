package io.github.teouk.fomo.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.teouk.fomo.FomoApplication
import io.github.teouk.fomo.data.repository.DataError
import io.github.teouk.fomo.data.repository.EventRepository
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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** The "When" buttons. ANY means no button is selected. */
enum class DatePreset { ANY, TODAY, WEEKEND, NEXT_7_DAYS, CUSTOM }

/** Everything the user has chosen on the Search screen. */
data class SearchFilters(
    val keyword: String = "",
    val categories: Set<Category> = emptySet(),
    val datePreset: DatePreset = DatePreset.ANY,
    /** Only used when [datePreset] is CUSTOM. Both days are included. */
    val customStart: LocalDate? = null,
    val customEnd: LocalDate? = null,
    val radiusMiles: Int = DEFAULT_RADIUS_MILES,
) {
    val isDefault: Boolean get() = this == SearchFilters()

    companion object {
        const val DEFAULT_RADIUS_MILES = 10
    }
}

data class SearchUiState(
    val filters: SearchFilters = SearchFilters(),
    val city: City = City.DEFAULT,
    val isLoading: Boolean = true,
    val results: List<EventListItem> = emptyList(),
    val error: DataError? = null,
) {
    val isEmpty: Boolean get() = !isLoading && error == null && results.isEmpty()
}

/**
 * Search & Filter. There is no Apply button (CW1 design): results refresh
 * [SEARCH_DEBOUNCE_MS] after the user stops changing filters.
 *
 * The filters live in [SavedStateHandle], so they survive rotation and the app being
 * killed in the background.
 */
@OptIn(FlowPreview::class)
class SearchViewModel(
    private val eventRepository: EventRepository,
    private val libraryRepository: LibraryRepository,
    private val settings: SettingsStore,
    private val savedStateHandle: SavedStateHandle,
    private val clock: Clock = Clock.systemUTC(),
) : ViewModel() {

    private val filters = MutableStateFlow(restoreFilters())
    private val refreshRequests = MutableStateFlow(0)

    private data class Loaded(
        val city: City = City.DEFAULT,
        val isLoading: Boolean = true,
        val events: List<Event> = emptyList(),
        val error: DataError? = null,
    )

    private val loaded = MutableStateFlow(Loaded())

    val uiState: StateFlow<SearchUiState> =
        combine(filters, loaded, libraryRepository.statuses) { filters, data, statuses ->
            SearchUiState(
                filters = filters,
                city = data.city,
                isLoading = data.isLoading,
                results = data.events.map { event ->
                    val lat = event.venue?.latitude
                    val lon = event.venue?.longitude
                    EventListItem(
                        event = event,
                        distanceMiles = if (lat != null && lon != null) {
                            Distance.miles(data.city.latitude, data.city.longitude, lat, lon)
                        } else {
                            event.distanceMiles
                        },
                        isSaved = statuses[event.id]?.isSaved == true,
                    )
                },
                error = data.error,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SearchUiState(filters = filters.value))

    init {
        viewModelScope.launch {
            combine(filters, settings.homeCity, refreshRequests) { filters, city, _ -> filters to city }
                // Wait until the user pauses: typing "jazz" sends one request, not four.
                // This keeps the app inside Data Thistle's monthly request quota.
                .debounce(SEARCH_DEBOUNCE_MS)
                .collectLatest { (filters, city) -> search(filters, city) }
        }
    }

    fun setKeyword(keyword: String) = updateFilters { it.copy(keyword = keyword) }

    fun setCategoryChecked(category: Category, checked: Boolean) = updateFilters {
        it.copy(categories = if (checked) it.categories + category else it.categories - category)
    }

    fun setDatePreset(preset: DatePreset) = updateFilters { it.copy(datePreset = preset) }

    fun setCustomRange(start: LocalDate, end: LocalDate) = updateFilters {
        it.copy(datePreset = DatePreset.CUSTOM, customStart = start, customEnd = end)
    }

    fun setRadius(miles: Int) = updateFilters {
        it.copy(radiusMiles = miles.coerceIn(EventFilter.MIN_RADIUS_MILES, EventFilter.MAX_RADIUS_MILES))
    }

    fun setCity(city: City) {
        viewModelScope.launch { settings.setHomeCity(city) }
    }

    fun clearAll() = updateFilters { SearchFilters() }

    fun retry() {
        refreshRequests.update { it + 1 }
    }

    fun toggleSave(event: Event) {
        viewModelScope.launch { libraryRepository.toggleSaved(event) }
    }

    private suspend fun search(filters: SearchFilters, city: City) {
        loaded.update { it.copy(city = city, isLoading = true, error = null) }
        val filter = EventFilter(
            latitude = city.latitude,
            longitude = city.longitude,
            radiusMiles = filters.radiusMiles,
            keyword = filters.keyword,
            categories = filters.categories,
            dateWindow = dateWindowFor(filters, ZonedDateTime.now(clock).withZoneSameInstant(UK_TIME_ZONE)),
        )
        loaded.value = when (val outcome = eventRepository.searchEvents(filter)) {
            is Outcome.Success -> Loaded(city, isLoading = false, events = outcome.data)
            is Outcome.Failure -> Loaded(city, isLoading = false, error = outcome.error)
        }
    }

    private fun updateFilters(change: (SearchFilters) -> SearchFilters) {
        filters.update(change)
        saveFilters(filters.value)
    }

    /** SavedStateHandle can only store simple types, so each field is saved separately. */
    private fun saveFilters(value: SearchFilters) {
        savedStateHandle[KEY_KEYWORD] = value.keyword
        savedStateHandle[KEY_CATEGORIES] = ArrayList(value.categories.map { it.name })
        savedStateHandle[KEY_DATE_PRESET] = value.datePreset.name
        savedStateHandle[KEY_CUSTOM_START] = value.customStart?.toEpochDay()
        savedStateHandle[KEY_CUSTOM_END] = value.customEnd?.toEpochDay()
        savedStateHandle[KEY_RADIUS] = value.radiusMiles
    }

    private fun restoreFilters(): SearchFilters = SearchFilters(
        keyword = savedStateHandle[KEY_KEYWORD] ?: "",
        categories = savedStateHandle.get<ArrayList<String>>(KEY_CATEGORIES).orEmpty()
            .mapNotNull { name -> Category.entries.firstOrNull { it.name == name } }.toSet(),
        datePreset = savedStateHandle.get<String>(KEY_DATE_PRESET)
            ?.let { name -> DatePreset.entries.firstOrNull { it.name == name } } ?: DatePreset.ANY,
        customStart = savedStateHandle.get<Long>(KEY_CUSTOM_START)?.let(LocalDate::ofEpochDay),
        customEnd = savedStateHandle.get<Long>(KEY_CUSTOM_END)?.let(LocalDate::ofEpochDay),
        radiusMiles = savedStateHandle[KEY_RADIUS] ?: SearchFilters.DEFAULT_RADIUS_MILES,
    )

    companion object {
        const val SEARCH_DEBOUNCE_MS = 500L
        private const val STOP_TIMEOUT_MS = 5_000L
        private val UK_TIME_ZONE: ZoneId = ZoneId.of("Europe/London")

        private const val KEY_KEYWORD = "keyword"
        private const val KEY_CATEGORIES = "categories"
        private const val KEY_DATE_PRESET = "date_preset"
        private const val KEY_CUSTOM_START = "custom_start"
        private const val KEY_CUSTOM_END = "custom_end"
        private const val KEY_RADIUS = "radius"

        /** The "When" choice as a time window, or null for "any date". Pure, so it is unit-tested. */
        fun dateWindowFor(filters: SearchFilters, now: ZonedDateTime): DateWindow? = when (filters.datePreset) {
            DatePreset.ANY -> null
            DatePreset.TODAY -> DateWindow.today(now)
            DatePreset.WEEKEND -> DateWindow.weekend(now)
            DatePreset.NEXT_7_DAYS -> DateWindow.next7Days(now)
            DatePreset.CUSTOM -> {
                val start = filters.customStart
                val end = filters.customEnd
                if (start != null && end != null) DateWindow.between(start, end, now.zone) else null
            }
        }

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as FomoApplication).container
                SearchViewModel(
                    eventRepository = container.eventRepository,
                    libraryRepository = container.libraryRepository,
                    settings = container.settings,
                    savedStateHandle = createSavedStateHandle(),
                )
            }
        }
    }
}
