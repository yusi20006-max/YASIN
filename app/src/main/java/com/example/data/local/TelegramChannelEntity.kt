package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "telegram_channels")
data class TelegramChannelEntity(
    @PrimaryKey val username: String,
    val titleFa: String,
    val isDefault: Boolean = false,
    val isEnabled: Boolean = true,
    val addedAt: Long = System.currentTimeMillis()
)
