package com.example.data.repository

import com.example.data.local.PressArticleEntity
import com.example.data.local.YasinDao
import kotlinx.coroutines.flow.Flow

class RoomPressRepository(
    private val dao: YasinDao
) : PressRepository {
    override fun observeArticles(): Flow<List<PressArticleEntity>> = dao.observeArticles()

    override suspend fun getArticleById(id: Long): PressArticleEntity? = dao.getArticleById(id)

    override suspend fun saveArticle(article: PressArticleEntity): Long {
        val now = System.currentTimeMillis()
        return if (article.id == 0L) {
            dao.insertArticle(article.copy(createdAt = now, updatedAt = now))
        } else {
            dao.updateArticle(article.copy(updatedAt = now))
            article.id
        }
    }

    override suspend fun deleteArticleById(id: Long) {
        dao.deleteArticleById(id)
    }

    override suspend fun togglePin(article: PressArticleEntity) {
        dao.updateArticle(
            article.copy(
                isPinned = !article.isPinned,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
