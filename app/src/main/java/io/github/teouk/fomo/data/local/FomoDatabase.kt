package io.github.teouk.fomo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SavedEventEntity::class], version = 1, exportSchema = false)
abstract class FomoDatabase : RoomDatabase() {
    abstract fun savedEventDao(): SavedEventDao

    companion object {
        private const val DATABASE_NAME = "fomo.db"

        fun create(context: Context): FomoDatabase =
            Room.databaseBuilder(context, FomoDatabase::class.java, DATABASE_NAME).build()
    }
}
