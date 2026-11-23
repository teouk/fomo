package io.github.teouk.fomo.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Room generates the SQL code for these functions at build time and checks every query
 * against the table, so a typo in a column name is a build error, not a crash.
 *
 * Functions returning [Flow] emit again whenever the table changes, so screens update
 * by themselves after a save. `suspend` functions run off the main thread.
 */
@Dao
interface SavedEventDao {
    @Query("SELECT * FROM saved_events")
    fun observeAll(): Flow<List<SavedEventEntity>>

    @Query("SELECT * FROM saved_events WHERE isSaved = 1 ORDER BY startsAtEpochMillis")
    fun observeSaved(): Flow<List<SavedEventEntity>>

    @Query("SELECT * FROM saved_events WHERE isPlanned = 1 ORDER BY startsAtEpochMillis")
    fun observePlanned(): Flow<List<SavedEventEntity>>

    @Query("SELECT * FROM saved_events WHERE id = :id")
    suspend fun get(id: String): SavedEventEntity?

    @Upsert
    suspend fun upsert(event: SavedEventEntity)

    @Query("DELETE FROM saved_events WHERE id = :id")
    suspend fun delete(id: String)
}
