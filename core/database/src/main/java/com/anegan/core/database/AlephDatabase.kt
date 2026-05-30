package com.anegan.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ConversionHistoryEntity::class,
        NoteEntity::class,
        VaultFileEntity::class,
        AudioTrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        AudioPlaybackStateEntity::class,
        ReadingProgressEntity::class,
        BookmarkEntity::class,
        SecondBrainNoteEntity::class,
        SecondBrainTagEntity::class,
        CurrencyRateEntity::class,
        CalculatorHistoryEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AneganDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun noteDao(): NoteDao
    abstract fun vaultFileDao(): VaultFileDao
    abstract fun audioTrackDao(): AudioTrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistTrackDao(): PlaylistTrackDao
    abstract fun audioPlaybackStateDao(): AudioPlaybackStateDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun secondBrainNoteDao(): SecondBrainNoteDao
    abstract fun secondBrainTagDao(): SecondBrainTagDao
    abstract fun currencyRateDao(): CurrencyRateDao
    abstract fun calculatorHistoryDao(): CalculatorHistoryDao
}

