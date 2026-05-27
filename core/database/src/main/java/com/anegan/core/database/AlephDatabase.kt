package com.anegan.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ConversionHistoryEntity::class, NoteEntity::class, VaultFileEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AneganDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun noteDao(): NoteDao
    abstract fun vaultFileDao(): VaultFileDao
}

