package io.github.teouk.fomo.data.repository

import io.github.teouk.fomo.data.local.SavedEventDao
import io.github.teouk.fomo.data.local.toEntity
import io.github.teouk.fomo.data.local.toEvent
import io.github.teouk.fomo.model.Event
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** What the user has done with an event. */
data class EventStatus(val isSaved: Boolean = false, val isPlanned: Boolean = false)

/**
 * The user's own events: saved (bookmarked) and planned. Backed by Room, so it works
 * offline and survives restarts. "Save" and "Add to plans" are separate actions,
 * as in the CW1 design.
 */
class LibraryRepository(private val dao: SavedEventDao) {
    val statuses: Flow<Map<String, EventStatus>> = dao.observeAll().map { rows ->
        rows.associate { it.id to EventStatus(it.isSaved, it.isPlanned) }
    }

    val savedEvents: Flow<List<Event>> = dao.observeSaved().map { rows -> rows.map { it.toEvent() } }

    val plannedEvents: Flow<List<Event>> = dao.observePlanned().map { rows -> rows.map { it.toEvent() } }

    /** @return true if the event is now saved. */
    suspend fun toggleSaved(event: Event): Boolean {
        val current = dao.get(event.id)
        val nowSaved = current?.isSaved != true
        write(event, isSaved = nowSaved, isPlanned = current?.isPlanned == true)
        return nowSaved
    }

    suspend fun setPlanned(event: Event, planned: Boolean) {
        val current = dao.get(event.id)
        write(event, isSaved = current?.isSaved == true, isPlanned = planned)
    }

    /** The stored copy of a saved or planned event, used when the API cannot be reached. */
    suspend fun getStored(eventId: String): Event? = dao.get(eventId)?.toEvent()

    /** A row with neither flag set means nothing, so it is deleted rather than kept. */
    private suspend fun write(event: Event, isSaved: Boolean, isPlanned: Boolean) {
        if (isSaved || isPlanned) {
            dao.upsert(event.toEntity(isSaved, isPlanned))
        } else {
            dao.delete(event.id)
        }
    }
}
