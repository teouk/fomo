package io.github.teouk.fomo.ui.discover

import androidx.lifecycle.SavedStateHandle
import io.github.teouk.fomo.FakeSavedEventDao
import io.github.teouk.fomo.MainDispatcherRule
import io.github.teouk.fomo.data.repository.EventRepository
import io.github.teouk.fomo.data.repository.LibraryRepository
import io.github.teouk.fomo.data.repository.SettingsStore
import io.github.teouk.fomo.data.sample.SampleTicketmasterService
import io.github.teouk.fomo.model.Category
import io.github.teouk.fomo.model.City
import kotlinx.coroutines.flow.MutableStateFlow
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

    private class FakeSettings : SettingsStore {
        override val homeCity = MutableStateFlow(City.SWANSEA)
        override val reminderLeadHours = MutableStateFlow(2)

        override suspend fun setHomeCity(city: City) {
            homeCity.value = city
        }

        override suspend fun setReminderLeadHours(hours: Int) {
            reminderLeadHours.value = hours
        }
    }

    // A Wednesday at noon, so the sample data has events today, this weekend and later.
    private val clock = Clock.fixed(Instant.parse("2026-11-11T12:00:00Z"), ZoneId.of("Europe/London"))
    private val settings = FakeSettings()
    private val library = LibraryRepository(FakeSavedEventDao())

    private fun viewModel() = DiscoverViewModel(
        eventRepository = EventRepository(
            SampleTicketmasterService(clock),
            hasApiKey = true,
            clock = clock,
            ioDispatcher = mainDispatcherRule.dispatcher,
        ),
        libraryRepository = library,
        settings = settings,
        savedStateHandle = SavedStateHandle(),
        usesSampleData = true,
        clock = clock,
    )

    @Test
    fun uiState_afterLoading_showsWeekendAndUpcomingEvents() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        val state = viewModel.uiState.first { !it.isLoading }
        // Fireworks and Comedy (Sat), the Charity Cup and the Indie Night (Sun).
        assertEquals(4, state.thisWeekend.size)
        assertEquals(8, state.nearYou.size)
        assertTrue(state.usesSampleData)
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
        val event = viewModel.uiState.first { !it.isLoading }.nearYou.first().event
        assertFalse(viewModel.uiState.value.nearYou.first().isSaved)
        viewModel.toggleSave(event)
        assertTrue(viewModel.uiState.first { it.nearYou.firstOrNull()?.isSaved == true }.nearYou.first().isSaved)
    }

    @Test
    fun selectCity_updatesLocationLine() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.selectCity(City.CARDIFF)
        assertEquals(City.CARDIFF, viewModel.uiState.first { it.city == City.CARDIFF && !it.isLoading }.city)
    }
}
