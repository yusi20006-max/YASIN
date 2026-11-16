package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface YasinDao {
    @Query("SELECT * FROM press_articles ORDER BY isPinned DESC, updatedAt DESC")
    fun observeArticles(): Flow<List<PressArticleEntity>>

    @Query("SELECT * FROM press_articles WHERE id = :id LIMIT 1")
    suspend fun getArticleById(id: Long): PressArticleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticle(article: PressArticleEntity): Long

    @Update
    suspend fun updateArticle(article: PressArticleEntity)

    @Query("DELETE FROM press_articles WHERE id = :id")
    suspend fun deleteArticleById(id: Long)

    @Query("SELECT * FROM ai_messages WHERE mode = :mode ORDER BY timestamp ASC, id ASC")
    fun observeMessagesByMode(mode: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM ai_messages WHERE mode = :mode")
    suspend fun clearMessagesByMode(mode: String)

    @Query("SELECT * FROM operation_logs ORDER BY timestamp DESC, id DESC LIMIT 300")
    fun observeLogs(): Flow<List<OperationLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: OperationLogEntity): Long

    @Query("DELETE FROM operation_logs")
    suspend fun clearLogs()
}
