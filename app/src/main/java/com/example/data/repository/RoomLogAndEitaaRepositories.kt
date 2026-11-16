package com.example.data.repository

import com.example.data.local.OperationLogEntity
import com.example.data.local.YasinDao
import com.example.data.remote.EitaaChannelPost
import com.example.data.remote.EitaaClient
import com.example.data.remote.EitaaPublishResult
import kotlinx.coroutines.flow.Flow

class RoomLogRepository(
    private val dao: YasinDao
) : LogRepository {
    override fun observeLogs(): Flow<List<OperationLogEntity>> = dao.observeLogs()

    override suspend fun log(module: String, level: String, title: String, details: String) {
        dao.insertLog(
            OperationLogEntity(
                module = module,
                level = level,
                title = title,
                details = details
            )
        )
    }

    override suspend fun clearLogs() {
        dao.clearLogs()
    }
}

class NetworkEitaaRepository(
    private val eitaaClient: EitaaClient
) : EitaaRepository {
    override suspend fun publishToEitaa(
        token: String,
        channelId: String,
        title: String,
        body: String
    ): EitaaPublishResult {
        val formattedText = buildString {
            if (title.isNotBlank()) {
                appendLine("**$title**")
                appendLine()
            }
            append(body.trim())
        }
        return eitaaClient.sendMessage(
            token = token,
            chatId = channelId,
            text = formattedText,
            title = title
        )
    }

    override suspend fun fetchChannelPosts(channelId: String): List<EitaaChannelPost> {
        return eitaaClient.fetchPublicChannelPosts(channelId)
    }
}
