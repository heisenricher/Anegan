package com.aleph.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversion_history")
data class ConversionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val originalFileName: String,
    val outputFileName: String,
    val originalFormat: String,
    val outputFormat: String,
    val status: String, // "SUCCESS", "FAILED"
    val timestamp: Long,
    val originalSize: Long,
    val outputSize: Long,
    val outputPath: String
)
