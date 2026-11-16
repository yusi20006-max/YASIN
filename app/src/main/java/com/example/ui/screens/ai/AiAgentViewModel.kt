package com.example.ui.screens.ai

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.registry.YasinModuleRegistry
import com.example.data.local.ChatMessageEntity
import com.example.data.local.PressArticleEntity
import com.example.data.local.YasinDao
import com.example.data.repository.AiGateway
import com.example.data.repository.EcosystemInspectorRepository
import com.example.data.repository.LogRepository
import com.example.data.repository.PressRepository
import com.example.data.repository.RelayConfigRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "AiAgentViewModel"

enum class AiWorkspaceMode(val dbKey: String, val labelFa: String) {
    CHAT("chat", "یاسین هوش (چت)"),
    AGENT("agent", "یاسین ایجنت (خودکار)")
}

sealed interface AiOperationState {
    data object Idle : AiOperationState
    data object Loading : AiOperationState
    data class Streaming(val partialText: String) : AiOperationState
    data class Error(val message: String) : AiOperationState
}

data class AiAgentUiState(
    val selectedMode: AiWorkspaceMode = AiWorkspaceMode.CHAT,
    val messages: List<ChatMessageEntity> = emptyList(),
    val operationState: AiOperationState = AiOperationState.Idle,
    val lastCreatedArticleId: Long? = null,
    val lastInspectedRepo: String? = null
)

class AiAgentViewModel(
    private val aiGateway: AiGateway,
    private val dao: YasinDao,
    private val pressRepository: PressRepository,
    private val inspectorRepository: EcosystemInspectorRepository,
    private val relayConfigRepository: RelayConfigRepository,
    private val logRepository: LogRepository
) : ViewModel() {

    private val modeFlow = MutableStateFlow(AiWorkspaceMode.CHAT)
    private val opStateFlow = MutableStateFlow<AiOperationState>(AiOperationState.Idle)
    private val lastCreatedArticleIdFlow = MutableStateFlow<Long?>(null)
    private val lastInspectedRepoFlow = MutableStateFlow<String?>(null)

    private val chatMessagesFlow = dao.observeMessagesByMode(AiWorkspaceMode.CHAT.dbKey)
    private val agentMessagesFlow = dao.observeMessagesByMode(AiWorkspaceMode.AGENT.dbKey)

    val uiState: StateFlow<AiAgentUiState> = combine(
        modeFlow,
        chatMessagesFlow,
        agentMessagesFlow,
        opStateFlow,
        lastCreatedArticleIdFlow,
        lastInspectedRepoFlow
    ) { values ->
        val mode = values[0] as AiWorkspaceMode
        @Suppress("UNCHECKED_CAST")
        val chatMsgs = values[1] as List<ChatMessageEntity>
        @Suppress("UNCHECKED_CAST")
        val agentMsgs = values[2] as List<ChatMessageEntity>
        val op = values[3] as AiOperationState
        val createdId = values[4] as Long?
        val inspectedRepo = values[5] as String?

        AiAgentUiState(
            selectedMode = mode,
            messages = if (mode == AiWorkspaceMode.CHAT) chatMsgs else agentMsgs,
            operationState = op,
            lastCreatedArticleId = createdId,
            lastInspectedRepo = inspectedRepo
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AiAgentUiState()
    )

    fun switchMode(mode: AiWorkspaceMode) {
        modeFlow.value = mode
        if (opStateFlow.value is AiOperationState.Error) {
            opStateFlow.value = AiOperationState.Idle
        }
    }

    fun dismissError() {
        if (opStateFlow.value is AiOperationState.Error) {
            opStateFlow.value = AiOperationState.Idle
        }
    }

    fun clearCurrentModeHistory() {
        val currentMode = modeFlow.value
        viewModelScope.launch {
            dao.clearMessagesByMode(currentMode.dbKey)
            opStateFlow.value = AiOperationState.Idle
        }
    }

    fun sendInput(rawInput: String) {
        val text = rawInput.trim()
        if (text.isEmpty() || opStateFlow.value is AiOperationState.Loading || opStateFlow.value is AiOperationState.Streaming) {
            return
        }

        if (modeFlow.value == AiWorkspaceMode.CHAT) {
            runStreamingChat(text)
        } else {
            runStructuredAgent(text)
        }
    }

    private fun runStreamingChat(prompt: String) {
        viewModelScope.launch {
            opStateFlow.value = AiOperationState.Loading
            val existingHistory = chatMessagesFlow.first()
            dao.insertMessage(
                ChatMessageEntity(
                    role = "user",
                    mode = AiWorkspaceMode.CHAT.dbKey,
                    content = prompt
                )
            )

            try {
                val systemPrompt = buildString {
                    appendLine("تو «یاسین هوش» هستی؛ دستیار فارسی، دقیق و مهندسی در اپلیکیشن بومی یاسین‌هاب.")
                    appendLine("اکوسیستم یاسین شامل ۹ ماژول است: Yasin Hub, Yasin AI, Yasin Agent, Yasin Press, Yasin MCP, YasinCore, Yasin CLI, Yasin Operation, Yasin Relay.")
                    appendLine("همیشه به زبان فارسی شفاف، ساختارمند و کاربردی پاسخ بده.")
                }

                var latestReply = ""
                aiGateway.streamChatReply(
                    history = existingHistory,
                    prompt = prompt,
                    systemInstruction = systemPrompt
                ).collect { partial ->
                    latestReply = partial
                    opStateFlow.value = AiOperationState.Streaming(partial)
                }

                dao.insertMessage(
                    ChatMessageEntity(
                        role = "model",
                        mode = AiWorkspaceMode.CHAT.dbKey,
                        content = latestReply
                    )
                )
                opStateFlow.value = AiOperationState.Idle
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Chat timed out", e)
                opStateFlow.value = AiOperationState.Error("زمان پاسخ‌گویی مدل به پایان رسید. لطفاً اتصال اینترنت را بررسی کرده و دوباره تلاش کنید.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Chat failed", e)
                opStateFlow.value = AiOperationState.Error(
                    e.localizedMessage ?: "خطا در ارتباط با Firebase AI Logic."
                )
            }
        }
    }

    private fun runStructuredAgent(goal: String) {
        viewModelScope.launch {
            opStateFlow.value = AiOperationState.Loading
            lastCreatedArticleIdFlow.value = null
            lastInspectedRepoFlow.value = null

            dao.insertMessage(
                ChatMessageEntity(
                    role = "user",
                    mode = AiWorkspaceMode.AGENT.dbKey,
                    content = goal
                )
            )

            try {
                val articles = pressRepository.observeArticles().first()
                val settings = relayConfigRepository.settingsFlow.first()
                val modulesDesc = YasinModuleRegistry.modules.joinToString("\n") {
                    "- ${it.codeName} (${it.titleFa}): ${it.executionMode.badgeFa}"
                }
                val contextSummary = """
                    مالک گیت‌هاب: ${settings.githubOwner}
                    تعداد نوشته‌های فعلی یاسین پرس: ${articles.size}
                    ماژول‌ها:
                    $modulesDesc
                """.trimIndent()

                val plan = aiGateway.planAgentAction(goal, contextSummary)

                when (plan.actionType) {
                    "CREATE_PRESS_DRAFT" -> {
                        val title = plan.articleTitle.ifBlank { "پیش‌نویس تولیدشده توسط یاسین ایجنت" }
                        val body = plan.articleBody.ifBlank { plan.replyFa }
                        val newId = pressRepository.saveArticle(
                            PressArticleEntity(
                                title = title,
                                body = body,
                                category = plan.articleCategory,
                                tags = "ایجنت,هوش مصنوعی",
                                isPublished = false
                            )
                        )
                        lastCreatedArticleIdFlow.value = newId
                        logRepository.log(
                            module = "Yasin Agent",
                            level = "SUCCESS",
                            title = "ایجاد پیش‌نویس جدید توسط ایجنت",
                            details = "عنوان: $title"
                        )
                        dao.insertMessage(
                            ChatMessageEntity(
                                role = "agent",
                                mode = AiWorkspaceMode.AGENT.dbKey,
                                content = "${plan.replyFa}\n\nعنوان ثبت‌شده در یاسین پرس: «$title»",
                                actionType = "CREATE_PRESS_DRAFT",
                                actionTarget = newId.toString()
                            )
                        )
                    }
                    "INSPECT_REPO" -> {
                        val repoName = plan.targetRepo.ifBlank { "Openfeed" }
                        val readmePreview = try {
                            inspectorRepository.fetchReadme(settings.githubOwner, repoName)
                                .take(900)
                        } catch (repoErr: Exception) {
                            "خطا در دریافت مستقیم README مخزن $repoName: ${repoErr.localizedMessage}"
                        }
                        lastInspectedRepoFlow.value = repoName
                        logRepository.log(
                            module = "Yasin Agent",
                            level = "INFO",
                            title = "بررسی مخزن توسط ایجنت",
                            details = "مخزن: ${settings.githubOwner}/$repoName"
                        )
                        dao.insertMessage(
                            ChatMessageEntity(
                                role = "agent",
                                mode = AiWorkspaceMode.AGENT.dbKey,
                                content = "${plan.replyFa}\n\nخلاصه دریافت‌شده از مخزن $repoName:\n$readmePreview",
                                actionType = "INSPECT_REPO",
                                actionTarget = repoName
                            )
                        )
                    }
                    else -> {
                        dao.insertMessage(
                            ChatMessageEntity(
                                role = "agent",
                                mode = AiWorkspaceMode.AGENT.dbKey,
                                content = plan.replyFa,
                                actionType = "ANSWER_ONLY"
                            )
                        )
                    }
                }
                opStateFlow.value = AiOperationState.Idle
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Agent timed out", e)
                logRepository.log(
                    module = "Yasin Agent",
                    level = "ERROR",
                    title = "پایان مهلت اجرای ایجنت",
                    details = "عملیات ایجنت بیش از ۶۰ ثانیه زمان برد."
                )
                opStateFlow.value = AiOperationState.Error("اجرای ایجنت بیش از ۶۰ ثانیه طول کشید. لطفاً دوباره امتحان کنید.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Agent execution failed", e)
                val err = e.localizedMessage ?: "خطا در اجرای دستور ایجنت."
                logRepository.log(
                    module = "Yasin Agent",
                    level = "ERROR",
                    title = "خطا در اجرای ایجنت",
                    details = err
                )
                opStateFlow.value = AiOperationState.Error(err)
            }
        }
    }

    companion object {
        fun factory(
            aiGateway: AiGateway,
            dao: YasinDao,
            pressRepository: PressRepository,
            inspectorRepository: EcosystemInspectorRepository,
            relayConfigRepository: RelayConfigRepository,
            logRepository: LogRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AiAgentViewModel(
                    aiGateway = aiGateway,
                    dao = dao,
                    pressRepository = pressRepository,
                    inspectorRepository = inspectorRepository,
                    relayConfigRepository = relayConfigRepository,
                    logRepository = logRepository
                ) as T
            }
        }
    }
}
