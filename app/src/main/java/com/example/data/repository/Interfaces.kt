package com.example.data.repository

import com.example.data.local.ChatMessageEntity
import com.example.data.local.OperationLogEntity
import com.example.data.local.PressArticleEntity
import com.example.data.remote.EitaaChannelPost
import com.example.data.remote.EitaaPublishResult
import com.example.data.remote.RelayPingResult
import kotlinx.coroutines.flow.Flow

data class RepoSummary(
    val id: Long,
    val name: String,
    val fullName: String,
    val description: String,
    val htmlUrl: String,
    val language: String,
    val stars: Int,
    val forks: Int,
    val updatedAt: String
)

data class AgentActionPlan(
    val actionType: String, // "CREATE_PRESS_DRAFT", "INSPECT_REPO", "ANSWER_ONLY"
    val replyFa: String,
    val articleTitle: String = "",
    val articleCategory: String = "فنی",
    val articleBody: String = "",
    val targetRepo: String = ""
)

data class RelaySettings(
    val relayBaseUrl: String = "",
    val githubOwner: String = "yusi20006-max",
    val darkTheme: Boolean = true,
    val autoAttachContext: Boolean = true,
    val eitaayarToken: String = "",
    val eitaaChannelId: String = ""
)

/**
 * Extensible AI Gateway contract.
 * Phase 1: Implemented by FirebaseAiLogicGateway (Gemini 3.8 Flash).
 * Phase 2+: Can seamlessly swap or route to Remote Yasin-Relay / Yasin-Agent endpoints.
 */
interface AiGateway {
    fun streamChatReply(
        history: List<ChatMessageEntity>,
        prompt: String,
        systemInstruction: String
    ): Flow<String>

    suspend fun generatePressAssist(
        instruction: String,
        currentTitle: String,
        currentBody: String
    ): String

    suspend fun planAgentAction(
        goal: String,
        ecosystemContext: String
    ): AgentActionPlan
}

/**
 * Extensible Content Studio repository.
 * Phase 1: Backed by RoomPressRepository (offline-first).
 * Phase 2+: Can sync with Yasin Press remote backend or Firestore.
 */
interface PressRepository {
    fun observeArticles(): Flow<List<PressArticleEntity>>
    suspend fun getArticleById(id: Long): PressArticleEntity?
    suspend fun saveArticle(article: PressArticleEntity): Long
    suspend fun deleteArticleById(id: Long)
    suspend fun togglePin(article: PressArticleEntity)
}

/**
 * Persistent operation and publishing log repository.
 */
interface LogRepository {
    fun observeLogs(): Flow<List<OperationLogEntity>>
    suspend fun log(module: String, level: String, title: String, details: String)
    suspend fun clearLogs()
}

/**
 * Eitaa & Eitaayar publishing + public channel check gateway.
 */
interface EitaaRepository {
    suspend fun publishToEitaa(
        token: String,
        channelId: String,
        title: String,
        body: String
    ): EitaaPublishResult

    suspend fun fetchChannelPosts(channelId: String): List<EitaaChannelPost>
}

/**
 * Extensible MCP / Docs Inspector repository.
 * Phase 1: Fetches live public repo metadata & READMEs via GitHub REST API.
 * Phase 2+: Can switch to Remote Yasin-MCP Streamable HTTP endpoint.
 */
interface EcosystemInspectorRepository {
    suspend fun fetchRepos(owner: String): List<RepoSummary>
    suspend fun fetchReadme(owner: String, repoName: String): String
}

/**
 * Persistent settings and Phase-2 Remote Relay Bridge configuration.
 */
interface RelayConfigRepository {
    val settingsFlow: Flow<RelaySettings>
    suspend fun updateRelayUrl(url: String)
    suspend fun updateGithubOwner(owner: String)
    suspend fun updateDarkTheme(dark: Boolean)
    suspend fun updateAutoAttachContext(enabled: Boolean)
    suspend fun updateEitaayarConfig(token: String, channelId: String)
    suspend fun pingRelay(url: String): RelayPingResult
}
