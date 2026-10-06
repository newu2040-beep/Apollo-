package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "export_history")
data class ExportHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalName: String,
    val outputName: String,
    val outputUriString: String,
    val sourceFormat: String,
    val targetFormat: String,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val timestamp: Long = System.currentTimeMillis()
)
