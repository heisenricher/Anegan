package com.anegan.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_files")
data class VaultFileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String, // "aadhaar", "pan", "passport", "insurance", "medical", "education", "legal", "tickets", "other"
    val size: Long,
    val mimeType: String,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val tags: String = "", // Comma-separated tags
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val encryptedFileName: String
)
