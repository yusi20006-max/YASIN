package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val role: String, // "user", "model", "agent"
    val mode: String, // "chat", "agent"
    val content: String,
    val actionType: String? = null,
    val actionTarget: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
