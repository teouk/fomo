package io.github.teouk.fomo.ui.plans

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.teouk.fomo.FomoApplication
import io.github.teouk.fomo.data.repository.EventStatus
import io.github.teouk.fomo.data.repository.LibraryRepository
import io.github.teouk.fomo.model.Event
import io.github.teouk.fomo.ui.common.EventListItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class PlansTab { SCHEDULE, SAVED }

/** A row in the My Plans list: either a day heading or an event. */
sealed interface PlansRow {
    data class DayHeader(val date: LocalDate?) : PlansRow

    data class EventRow(val item: EventListItem) : PlansRow
}

data class PlansUiState(
    val tab: PlansTab = PlansTab.SCHEDULE,
    val rows: List<PlansRow> = emptyList(),
) {
    val isEmpty: Boolean get() = rows.isEmpty()
}

class PlansViewModel(
    private val libraryRepository: LibraryRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val tab = savedStateHandle.getStateFlow(KEY_TAB, PlansTab.SCHEDULE)

    val uiState: StateFlow<PlansUiState> = combine(
        tab,
        libraryRepository.plannedEvents,
        libraryRepository.savedEvents,
        libraryRepository.statuses,
    ) { tab, planned, saved, statuses ->
        val rows = when (tab) {
            PlansTab.SCHEDULE -> groupByDay(planned, statuses)
            PlansTab.SAVED -> saved.map { PlansRow.EventRow(it.toItem(statuses)) }
        }
        PlansUiState(tab, rows)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PlansUiState())

    fun selectTab(tab: PlansTab) {
        savedStateHandle[KEY_TAB] = tab
    }

    fun toggleSave(event: Event) {
        viewModelScope.launch { libraryRepository.toggleSaved(event) }
    }

    companion object {
        private const val KEY_TAB = "tab"
        private const val STOP_TIMEOUT_MS = 5_000L

        /** Planned events arrive sorted by start time; a heading is added before each new day. */
        fun groupByDay(events: List<Event>, statuses: Map<String, EventStatus>): List<PlansRow> =
            events.groupBy { it.localDate }.flatMap { (date, dayEvents) ->
                listOf(PlansRow.DayHeader(date)) + dayEvents.map { PlansRow.EventRow(it.toItem(statuses)) }
            }

        private fun Event.toItem(statuses: Map<String, EventStatus>) =
            EventListItem(this, distanceMiles = null, isSaved = statuses[id]?.isSaved == true)

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as FomoApplication).container
                PlansViewModel(container.libraryRepository, createSavedStateHandle())
            }
        }
    }
}
