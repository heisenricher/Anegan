package com.anegan.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isChecklist: Boolean = false,
    val colorLabel: String = "none",   // "none","red","orange","yellow","green","blue","purple"
    val hasReminder: Boolean = false,
    val reminderTime: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
