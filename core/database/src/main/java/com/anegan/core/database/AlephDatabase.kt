package com.anegan.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ConversionHistoryEntity::class], version = 1, exportSchema = false)
abstract class AneganDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}
