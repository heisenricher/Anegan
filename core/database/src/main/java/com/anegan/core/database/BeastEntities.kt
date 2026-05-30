package com.anegan.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_tracks")
data class AudioTrackEntity(
    @PrimaryKey val filePath: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val albumArtPath: String? = null,
    val playCount: Int = 0,
    val lastPlayedAt: Long? = null,
    val isFavorite: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String, // UUID
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_tracks")
data class PlaylistTrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: String,
    val trackPath: String,
    val position: Int
)

@Entity(tableName = "audio_playback_state")
data class AudioPlaybackStateEntity(
    @PrimaryKey val id: Int = 1, // Singleton row
    val currentTrackPath: String?,
    val currentPosition: Long,
    val currentPlaylistId: String?,
    val shuffleEnabled: Boolean,
    val repeatMode: Int // 0 = off, 1 = all, 2 = one
)

@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey val filePath: String,
    val fileName: String,
    val fileType: String, // "pdf", "epub", "docx", "txt"
    val currentPage: Int,
    val totalPages: Int,
    val lastReadAt: Long = System.currentTimeMillis(),
    val percentComplete: Float
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val filePath: String,
    val pageNumber: Int,
    val label: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "second_brain_notes")
data class SecondBrainNoteEntity(
    @PrimaryKey val id: String, // UUID
    val title: String,
    val content: String,
    val parentId: String? = null,
    val tags: String = "", // Comma-separated
    val linkedNoteIds: String = "", // Comma-separated
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "second_brain_tags")
data class SecondBrainTagEntity(
    @PrimaryKey val name: String,
    val color: String = "#MidnightIndigo",
    val noteCount: Int = 0
)

@Entity(tableName = "currency_rates")
data class CurrencyRateEntity(
    @PrimaryKey val code: String, // "USD", "EUR", etc.
    val name: String,
    val rateToUsd: Double,
    val manualRate: Double? = null,
    val lastUpdatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "calculator_history")
data class CalculatorHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val expression: String,
    val result: String,
    val mode: String, // "basic", "scientific"
    val timestamp: Long = System.currentTimeMillis()
)
