package io.github.teouk.fomo.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.teouk.fomo.FomoApplication
import io.github.teouk.fomo.data.location.GeoPoint
import io.github.teouk.fomo.data.location.LocationRepository
import io.github.teouk.fomo.data.repository.DataError
import io.github.teouk.fomo.data.repository.EventRepository
import io.github.teouk.fomo.data.repository.EventStatus
import io.github.teouk.fomo.data.repository.LibraryRepository
import io.github.teouk.fomo.data.repository.Outcome
import io.github.teouk.fomo.data.repository.SettingsStore
import io.github.teouk.fomo.model.City
import io.github.teouk.fomo.model.Event
import io.github.teouk.fomo.model.EventFilter
import io.github.teouk.fomo.ui.common.EventListItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Where the map is centred, which decides the heading on screen. */
enum class MapCentre {
    DEVICE_LOCATION,

    /** Location permission not given. */
    HOME_TOWN,

    /** Permission given, but the phone could not find its position in time. */
    HOME_TOWN_NO_FIX,
    HOME_TOWN_OUTSIDE_UK,
    CHOSEN_AREA,
}

/** All events at one venue: one marker on the map. */
data class VenueMarker(
    val key: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val events: List<EventListItem>,
)

data class MapUiState(
    val centre: GeoPoint? = null,
    val centreSource: MapCentre = MapCentre.HOME_TOWN,
    val homeCity: City = City.DEFAULT,
    val isLoading: Boolean = false,
    val venues: List<VenueMarker> = emptyList(),
    val error: DataError? = null,
    val providerName: String = "",
) {
    val isEmpty: Boolean get() = centre != null && !isLoading && error == null && venues.isEmpty()
}

class MapViewModel(
    private val eventRepository: EventRepository,
    private val libraryRepository: LibraryRepository,
    private val locationRepository: LocationRepository,
    private val settings: SettingsStore,
) : ViewModel() {

    private data class Loaded(
        val centre: GeoPoint? = null,
        val centreSource: MapCentre = MapCentre.HOME_TOWN,
        val homeCity: City = City.DEFAULT,
        val isLoading: Boolean = false,
        val events: List<Event> = emptyList(),
        val error: DataError? = null,
    )

    private val loaded = MutableStateFlow(Loaded())
    private var started = false
    private var searchJob: Job? = null

    val uiState: StateFlow<MapUiState> = combine(loaded, libraryRepository.statuses) { data, statuses ->
        MapUiState(
            centre = data.centre,
            centreSource = data.centreSource,
            homeCity = data.homeCity,
            isLoading = data.isLoading,
            venues = groupByVenue(data.events, statuses),
            error = data.error,
            providerName = eventRepository.providerName,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MapUiState())

    /**
     * Called once the location permission question has been answered. Centres on the
     * phone's position if allowed and available, otherwise on the home town.
     * Calling it again (for example after rotation) does nothing.
     */
    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            val homeCity = settings.homeCity.first()
            val device = locationRepository.currentLocation()
            val homeTown = GeoPoint(homeCity.latitude, homeCity.longitude)
            when {
                device == null && locationRepository.hasPermission() ->
                    search(homeTown, MapCentre.HOME_TOWN_NO_FIX, homeCity)
                device == null -> search(homeTown, MapCentre.HOME_TOWN, homeCity)
                // Data Thistle only lists UK events, so searching abroad would always be empty.
                !device.isInUk -> search(homeTown, MapCentre.HOME_TOWN_OUTSIDE_UK, homeCity)
                else -> search(device, MapCentre.DEVICE_LOCATION, homeCity)
            }
        }
    }

    /** The user panned the map and tapped "Search this area". */
    fun searchArea(centre: GeoPoint) {
        search(centre, MapCentre.CHOSEN_AREA, loaded.value.homeCity)
    }

    fun retry() {
        val current = loaded.value
        current.centre?.let { search(it, current.centreSource, current.homeCity) }
    }

    private fun search(centre: GeoPoint, source: MapCentre, homeCity: City) {
        // A newer search replaces an older one that has not finished yet.
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            loaded.update { it.copy(centre = centre, centreSource = source, homeCity = homeCity, isLoading = true, error = null) }
            val filter = EventFilter(centre.latitude, centre.longitude, radiusMiles = SEARCH_RADIUS_MILES)
            loaded.update {
                when (val outcome = eventRepository.searchEvents(filter)) {
                    is Outcome.Success -> it.copy(isLoading = false, events = outcome.data)
                    is Outcome.Failure -> it.copy(isLoading = false, events = emptyList(), error = outcome.error)
                }
            }
        }
    }

    companion object {
        /** Wide enough to fill a city-level map, small enough to stay relevant. */
        const val SEARCH_RADIUS_MILES = 10
        private const val STOP_TIMEOUT_MS = 5_000L

        /**
         * One marker per venue: a cinema showing ten films would otherwise be ten markers
         * stacked on the same spot, and only the top one could be tapped.
         * Events without coordinates cannot be placed, so they are left out.
         */
        fun groupByVenue(events: List<Event>, statuses: Map<String, EventStatus>): List<VenueMarker> =
            events
                .filter { it.venue?.latitude != null && it.venue.longitude != null }
                .groupBy { "${it.venue!!.name}|${it.venue.latitude}|${it.venue.longitude}" }
                .map { (key, venueEvents) ->
                    val venue = venueEvents.first().venue!!
                    VenueMarker(
                        key = key,
                        name = venue.name,
                        latitude = venue.latitude!!,
                        longitude = venue.longitude!!,
                        events = venueEvents.map {
                            EventListItem(it, distanceMiles = it.distanceMiles, isSaved = statuses[it.id]?.isSaved == true)
                        },
                    )
                }

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as FomoApplication).container
                MapViewModel(
                    eventRepository = container.eventRepository,
                    libraryRepository = container.libraryRepository,
                    locationRepository = container.locationRepository,
                    settings = container.settings,
                )
            }
        }
    }

    fun toggleSave(event: Event) {
        viewModelScope.launch { libraryRepository.toggleSaved(event) }
    }
}
