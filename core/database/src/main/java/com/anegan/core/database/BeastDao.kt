package com.anegan.core.database

import androidx.room.*

@Dao
interface AudioTrackDao {
    @Query("SELECT * FROM audio_tracks ORDER BY addedAt DESC")
    suspend fun getAll(): List<AudioTrackEntity>

    @Query("SELECT * FROM audio_tracks WHERE filePath = :filePath LIMIT 1")
    suspend fun getByPath(filePath: String): AudioTrackEntity?

    @Query("SELECT * FROM audio_tracks WHERE isFavorite = 1 ORDER BY addedAt DESC")
    suspend fun getFavorites(): List<AudioTrackEntity>

    @Query("SELECT * FROM audio_tracks ORDER BY playCount DESC LIMIT :limit")
    suspend fun getMostPlayed(limit: Int): List<AudioTrackEntity>

    @Upsert
    suspend fun upsert(track: AudioTrackEntity)

    @Delete
    suspend fun delete(track: AudioTrackEntity)
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    suspend fun getAll(): List<PlaylistEntity>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PlaylistEntity?

    @Upsert
    suspend fun upsert(playlist: PlaylistEntity)

    @Delete
    suspend fun delete(playlist: PlaylistEntity)
}

@Dao
interface PlaylistTrackDao {
    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getTracksForPlaylist(playlistId: String): List<PlaylistTrackEntity>

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun deletePlaylistTracks(playlistId: String)

    @Upsert
    suspend fun upsert(track: PlaylistTrackEntity)

    @Delete
    suspend fun delete(track: PlaylistTrackEntity)
}

@Dao
interface AudioPlaybackStateDao {
    @Query("SELECT * FROM audio_playback_state WHERE id = 1 LIMIT 1")
    suspend fun getState(): AudioPlaybackStateEntity?

    @Upsert
    suspend fun upsert(state: AudioPlaybackStateEntity)
}

@Dao
interface ReadingProgressDao {
    @Query("SELECT * FROM reading_progress ORDER BY lastReadAt DESC")
    suspend fun getAll(): List<ReadingProgressEntity>

    @Query("SELECT * FROM reading_progress WHERE filePath = :filePath LIMIT 1")
    suspend fun getByPath(filePath: String): ReadingProgressEntity?

    @Upsert
    suspend fun upsert(progress: ReadingProgressEntity)

    @Query("DELETE FROM reading_progress WHERE filePath = :filePath")
    suspend fun deleteByPath(filePath: String)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE filePath = :filePath ORDER BY pageNumber ASC")
    suspend fun getBookmarksForFile(filePath: String): List<BookmarkEntity>

    @Upsert
    suspend fun upsert(bookmark: BookmarkEntity)

    @Delete
    suspend fun delete(bookmark: BookmarkEntity)
}

@Dao
interface SecondBrainNoteDao {
    @Query("SELECT * FROM second_brain_notes ORDER BY isPinned DESC, updatedAt DESC")
    suspend fun getAll(): List<SecondBrainNoteEntity>

    @Query("SELECT * FROM second_brain_notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SecondBrainNoteEntity?

    @Upsert
    suspend fun upsert(note: SecondBrainNoteEntity)

    @Delete
    suspend fun delete(note: SecondBrainNoteEntity)

    @Query("SELECT * FROM second_brain_notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<SecondBrainNoteEntity>
}

@Dao
interface SecondBrainTagDao {
    @Query("SELECT * FROM second_brain_tags ORDER BY name ASC")
    suspend fun getAll(): List<SecondBrainTagEntity>

    @Upsert
    suspend fun upsert(tag: SecondBrainTagEntity)

    @Delete
    suspend fun delete(tag: SecondBrainTagEntity)
}

@Dao
interface CurrencyRateDao {
    @Query("SELECT * FROM currency_rates ORDER BY code ASC")
    suspend fun getAll(): List<CurrencyRateEntity>

    @Query("SELECT * FROM currency_rates WHERE code = :code LIMIT 1")
    suspend fun getByCode(code: String): CurrencyRateEntity?

    @Upsert
    suspend fun upsert(rate: CurrencyRateEntity)
}

@Dao
interface CalculatorHistoryDao {
    @Query("SELECT * FROM calculator_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<CalculatorHistoryEntity>

    @Upsert
    suspend fun upsert(entry: CalculatorHistoryEntity)

    @Query("DELETE FROM calculator_history")
    suspend fun clearAll()
}
