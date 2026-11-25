package io.github.teouk.fomo.ui.search

import androidx.lifecycle.SavedStateHandle
import io.github.teouk.fomo.FakeEventDataSource
import io.github.teouk.fomo.FakeEventDataSource.Companion.event
import io.github.teouk.fomo.FakeSavedEventDao
import io.github.teouk.fomo.FakeSettings
import io.github.teouk.fomo.MainDispatcherRule
import io.github.teouk.fomo.data.repository.EventRepository
import io.github.teouk.fomo.data.repository.LibraryRepository
import io.github.teouk.fomo.model.Category
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    // A StandardTestDispatcher lets the test move virtual time, so the 500 ms debounce
    // runs instantly but in the right order.
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val clock = Clock.fixed(Instant.parse("2026-11-11T12:00:00Z"), ZoneId.of("Europe/London"))
    private val dataSource = FakeEventDataSource(
        listOf(
            event("jazz", "Jazz on the Bay", "2026-11-11T19:30:00Z") to Category.MUSIC,
            event("comedy", "Comedy Club Night", "2026-11-14T20:00:00Z") to Category.COMEDY,
            event("film", "Classic Noir", "2026-11-18T19:00:00Z") to Category.FILM,
        ),
    )

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) = SearchViewModel(
        eventRepository = EventRepository(dataSource, hasApiKey = true, clock = clock, ioDispatcher = mainDispatcherRule.dispatcher),
        libraryRepository = LibraryRepository(FakeSavedEventDao()),
        settings = FakeSettings(),
        savedStateHandle = handle,
        posterReader = posterReader,
        clock = clock,
    )

    /** Pretends to be ML Kit: returns whatever lines the test sets. */
    private class FakePosterReader : io.github.teouk.fomo.data.ocr.PosterReader {
        var lines: List<String> = emptyList()
        var failure: Exception? = null

        override suspend fun readLines(photoUri: String): List<String> {
            failure?.let { throw it }
            return lines
        }
    }

    private val posterReader = FakePosterReader()

    @Test
    fun setKeyword_typedQuickly_sendsOneRequestAfterPause() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        val callsBefore = dataSource.searchCalls

        "jazz".indices.forEach { i ->
            viewModel.setKeyword("jazz".substring(0, i + 1))
            advanceTimeBy(100)
        }
        assertEquals(callsBefore, dataSource.searchCalls)

        advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS + 1)
        advanceUntilIdle()
        assertEquals(callsBefore + 1, dataSource.searchCalls)
        assertEquals(listOf("jazz"), viewModel.uiState.value.results.map { it.event.id })
    }

    @Test
    fun setCategoryChecked_twoCategories_showsEither() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.setCategoryChecked(Category.MUSIC, true)
        viewModel.setCategoryChecked(Category.FILM, true)
        advanceUntilIdle()
        assertEquals(setOf("jazz", "film"), viewModel.uiState.value.results.map { it.event.id }.toSet())
    }

    @Test
    fun clearAll_afterFilters_returnsToDefaults() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.setKeyword("noir")
        viewModel.setRadius(40)
        viewModel.clearAll()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.filters.isDefault)
        assertEquals(3, viewModel.uiState.value.results.size)
    }

    @Test
    fun filters_afterProcessDeath_areRestoredFromSavedState() = runTest(mainDispatcherRule.dispatcher) {
        val handle = SavedStateHandle()
        val first = viewModel(handle)
        first.setKeyword("comedy")
        first.setCategoryChecked(Category.COMEDY, true)
        first.setRadius(30)

        // A new ViewModel built from the same SavedStateHandle simulates Android
        // recreating the screen after killing the app in the background.
        val restored = viewModel(handle).uiState.value.filters
        assertEquals("comedy", restored.keyword)
        assertEquals(setOf(Category.COMEDY), restored.categories)
        assertEquals(30, restored.radiusMiles)
    }

    @Test
    fun readPoster_photoWithText_offersCleanedLines() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        posterReader.lines = listOf("THE HARBOUR LIGHTS", "£15", "BRANGWYN HALL")
        viewModel.readPoster("content://poster")
        advanceUntilIdle()
        assertEquals(PosterState.Lines(listOf("THE HARBOUR LIGHTS", "BRANGWYN HALL")), viewModel.uiState.value.poster)
    }

    @Test
    fun readPoster_noUsefulText_reportsNoText() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        posterReader.lines = listOf("£15", "~")
        viewModel.readPoster("content://poster")
        advanceUntilIdle()
        assertEquals(PosterState.NoText, viewModel.uiState.value.poster)
    }

    @Test
    fun readPoster_unreadableImage_reportsFailed() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        posterReader.failure = java.io.IOException("corrupt image")
        viewModel.readPoster("content://poster")
        advanceUntilIdle()
        assertEquals(PosterState.Failed, viewModel.uiState.value.poster)
    }

    @Test
    fun dateWindowFor_presets_mapToWindows() {
        val now = ZonedDateTime.parse("2026-11-11T12:00:00Z[Europe/London]")
        assertNull(SearchViewModel.dateWindowFor(SearchFilters(), now))
        val weekend = SearchViewModel.dateWindowFor(SearchFilters(datePreset = DatePreset.WEEKEND), now)!!
        assertEquals(Instant.parse("2026-11-14T00:00:00Z"), weekend.start)
        val custom = SearchViewModel.dateWindowFor(
            SearchFilters(
                datePreset = DatePreset.CUSTOM,
                customStart = LocalDate.of(2026, 11, 20),
                customEnd = LocalDate.of(2026, 11, 21),
            ),
            now,
        )!!
        assertEquals(Instant.parse("2026-11-20T00:00:00Z"), custom.start)
        assertEquals(Instant.parse("2026-11-22T00:00:00Z"), custom.end)
    }
}
