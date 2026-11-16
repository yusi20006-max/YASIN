package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "operation_logs")
data class OperationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val module: String, // e.g. "Yasin Press", "Eitaayar", "Yasin Agent", "Yasin Relay"
    val level: String,  // "SUCCESS", "INFO", "ERROR"
    val title: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)
