package io.github.teouk.fomo

import io.github.teouk.fomo.data.local.SavedEventDao
import io.github.teouk.fomo.data.local.SavedEventEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** An in-memory stand-in for the Room DAO, for fast unit tests without a database. */
class FakeSavedEventDao : SavedEventDao {
    private val rows = MutableStateFlow<Map<String, SavedEventEntity>>(emptyMap())

    override fun observeAll(): Flow<List<SavedEventEntity>> = rows.map { it.values.toList() }

    override fun observeSaved() = rows.map { all -> all.values.filter { it.isSaved }.sortedBy { it.startsAtEpochMillis } }

    override fun observePlanned() = rows.map { all -> all.values.filter { it.isPlanned }.sortedBy { it.startsAtEpochMillis } }

    override suspend fun get(id: String) = rows.value[id]

    override suspend fun upsert(event: SavedEventEntity) {
        rows.value = rows.value + (event.id to event)
    }

    override suspend fun delete(id: String) {
        rows.value = rows.value - id
    }
}
