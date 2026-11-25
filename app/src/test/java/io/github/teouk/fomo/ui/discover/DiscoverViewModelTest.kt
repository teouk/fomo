package io.github.teouk.fomo.ui.discover

import androidx.lifecycle.SavedStateHandle
import io.github.teouk.fomo.FakeEventDataSource
import io.github.teouk.fomo.FakeEventDataSource.Companion.event
import io.github.teouk.fomo.FakeSavedEventDao
import io.github.teouk.fomo.FakeSettings
import io.github.teouk.fomo.MainDispatcherRule
import io.github.teouk.fomo.data.repository.EventRepository
import io.github.teouk.fomo.data.repository.LibraryRepository
import io.github.teouk.fomo.model.Category
import io.github.teouk.fomo.model.City
import io.github.teouk.fomo.model.Venue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class DiscoverViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // Wednesday 11 Nov 2026 at noon.
    private val clock = Clock.fixed(Instant.parse("2026-11-11T12:00:00Z"), ZoneId.of("Europe/London"))
    private val arena = Venue("Swansea Arena", "Swansea", null, 51.6144, -3.9436)
    private val dataSource = FakeEventDataSource(
        listOf(
            event("jazz", "Jazz on the Bay", "2026-11-11T19:30:00Z", arena) to Category.MUSIC,
            event("comedy", "Comedy Club Night", "2026-11-14T20:00:00Z", arena) to Category.COMEDY,
            event("cup", "Charity Cup", "2026-11-15T11:00:00Z") to Category.SPORT,
            event("film", "Classic Noir", "2026-11-18T19:00:00Z") to Category.FILM,
        ),
    )
    private val settings = FakeSettings()
    private val library = LibraryRepository(FakeSavedEventDao())

    private fun viewModel() = DiscoverViewModel(
        eventRepository = EventRepository(dataSource, hasApiKey = true, clock = clock, ioDispatcher = mainDispatcherRule.dispatcher),
        libraryRepository = library,
        settings = settings,
        savedStateHandle = SavedStateHandle(),
        clock = clock,
    )

    @Test
    fun uiState_afterLoading_splitsWeekendFromUpcoming() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        val state = viewModel.uiState.first { !it.isLoading }
        assertEquals(listOf("comedy", "cup"), state.thisWeekend.map { it.event.id })
        assertEquals(4, state.nearYou.size)
        assertEquals("Fake", state.providerName)
    }

    @Test
    fun uiState_venueWithCoordinates_hasDistanceFromCity() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        val jazz = viewModel.uiState.first { !it.isLoading }.nearYou.first { it.event.id == "jazz" }
        assertEquals(0.5, jazz.distanceMiles!!, 0.1)
    }

    @Test
    fun selectCategory_comedy_showsOnlyComedy() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.selectCategory(Category.COMEDY)
        val state = viewModel.uiState.first { !it.isLoading && it.category == Category.COMEDY }
        assertEquals(listOf("Comedy Club Night"), state.nearYou.map { it.event.name })
    }

    @Test
    fun toggleSave_marksEventAsSaved() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        val first = viewModel.uiState.first { !it.isLoading }.nearYou.first()
        assertFalse(first.isSaved)
        viewModel.toggleSave(first.event)
        assertTrue(viewModel.uiState.first { it.nearYou.first().isSaved }.nearYou.first().isSaved)
    }

    @Test
    fun selectCity_searchesAroundNewCity() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.selectCity(City.CARDIFF)
        viewModel.uiState.first { it.city == City.CARDIFF && !it.isLoading }
        assertEquals(City.CARDIFF.latitude, dataSource.searchedFilters.last().latitude, 0.0001)
    }
}
