package com.anegan.core.database

import androidx.room.*

@Dao
interface VaultFileDao {
    @Query("SELECT * FROM vault_files ORDER BY isPinned DESC, createdAt DESC")
    suspend fun getAll(): List<VaultFileEntity>

    @Query("SELECT * FROM vault_files WHERE category = :category ORDER BY isPinned DESC, createdAt DESC")
    suspend fun getByCategory(category: String): List<VaultFileEntity>

    @Query("SELECT * FROM vault_files WHERE isPinned = 1 ORDER BY createdAt DESC")
    suspend fun getPinned(): List<VaultFileEntity>

    @Query("SELECT * FROM vault_files ORDER BY updatedAt DESC LIMIT 5")
    suspend fun getRecent(): List<VaultFileEntity>

    @Query("SELECT * FROM vault_files WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): VaultFileEntity?

    @Upsert
    suspend fun upsert(file: VaultFileEntity)

    @Query("DELETE FROM vault_files WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM vault_files WHERE name LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<VaultFileEntity>
}
