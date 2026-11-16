package com.example.ui.screens.press

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.OperationLogEntity
import com.example.data.local.PressArticleEntity
import com.example.data.remote.EitaaChannelPost
import com.example.data.repository.AiGateway
import com.example.data.repository.EitaaRepository
import com.example.data.repository.LogRepository
import com.example.data.repository.PressRepository
import com.example.data.repository.RelayConfigRepository
import com.example.data.repository.RelaySettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "PressViewModel"

enum class PressSectionTab(val labelFa: String) {
    ARTICLES("نوشته‌ها"),
    TEXT_ARCHIVE("کل انتشارات (متن)"),
    LOGS("ثبت لاگ")
}

enum class PressFilter(val labelFa: String) {
    ALL("همه"),
    DRAFTS("پیش‌نویس"),
    PUBLISHED("منتشرشده"),
    PINNED("سنجاق‌شده")
}

enum class LogLevelFilter(val labelFa: String) {
    ALL("همه"),
    SUCCESS("موفق"),
    ERROR("خطا")
}

enum class TextArchiveSource(val labelFa: String) {
    LOCAL_PUBLICATIONS("متن یکجای انتشارات یاسین پرس"),
    EITAA_CHANNEL("پست‌های زنده کانال ایتا")
}

data class PressEditorState(
    val isOpen: Boolean = false,
    val editingId: Long = 0L,
    val title: String = "",
    val body: String = "",
    val category: String = "فنی",
    val tags: String = "",
    val isPublished: Boolean = false,
    val isAiAssisting: Boolean = false,
    val aiAssistError: String? = null
)

data class PressUiState(
    val selectedTab: PressSectionTab = PressSectionTab.ARTICLES,
    val articles: List<PressArticleEntity> = emptyList(),
    val allArticles: List<PressArticleEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: PressFilter = PressFilter.ALL,
    val editor: PressEditorState = PressEditorState(),
    val settings: RelaySettings = RelaySettings(),
    val publishingArticleId: Long? = null,
    val statusBanner: String? = null,
    val errorBanner: String? = null,
    val archiveSource: TextArchiveSource = TextArchiveSource.LOCAL_PUBLICATIONS,
    val eitaaPosts: List<EitaaChannelPost> = emptyList(),
    val isLoadingEitaaPosts: Boolean = false,
    val eitaaFetchError: String? = null,
    val logs: List<OperationLogEntity> = emptyList(),
    val logFilter: LogLevelFilter = LogLevelFilter.ALL
)

class PressViewModel(
    private val pressRepository: PressRepository,
    private val aiGateway: AiGateway,
    private val eitaaRepository: EitaaRepository,
    private val relayConfigRepository: RelayConfigRepository,
    private val logRepository: LogRepository
) : ViewModel() {

    private val tabFlow = MutableStateFlow(PressSectionTab.ARTICLES)
    private val searchFlow = MutableStateFlow("")
    private val filterFlow = MutableStateFlow(PressFilter.ALL)
    private val editorFlow = MutableStateFlow(PressEditorState())
    private val publishingIdFlow = MutableStateFlow<Long?>(null)
    private val statusBannerFlow = MutableStateFlow<String?>(null)
    private val errorBannerFlow = MutableStateFlow<String?>(null)
    private val archiveSourceFlow = MutableStateFlow(TextArchiveSource.LOCAL_PUBLICATIONS)
    private val eitaaPostsFlow = MutableStateFlow<List<EitaaChannelPost>>(emptyList())
    private val isLoadingEitaaFlow = MutableStateFlow(false)
    private val eitaaErrorFlow = MutableStateFlow<String?>(null)
    private val logFilterFlow = MutableStateFlow(LogLevelFilter.ALL)

    private val baseDataFlow = combine(
        pressRepository.observeArticles(),
        relayConfigRepository.settingsFlow,
        logRepository.observeLogs()
    ) { articles, settings, logs ->
        Triple(articles, settings, logs)
    }

    private val uiControlsFlow = combine(
        tabFlow,
        searchFlow,
        filterFlow,
        editorFlow,
        publishingIdFlow
    ) { tab, query, filter, editor, publishingId ->
        listOf(tab, query, filter, editor, publishingId)
    }

    private val feedAndLogsControlsFlow = combine(
        statusBannerFlow,
        errorBannerFlow,
        archiveSourceFlow,
        eitaaPostsFlow,
        isLoadingEitaaFlow,
        eitaaErrorFlow,
        logFilterFlow
    ) { values ->
        values
    }

    val uiState: StateFlow<PressUiState> = combine(
        baseDataFlow,
        uiControlsFlow,
        feedAndLogsControlsFlow
    ) { (articles, settings, logs), controls, feedValues ->
        val tab = controls[0] as PressSectionTab
        val query = controls[1] as String
        val filter = controls[2] as PressFilter
        val editor = controls[3] as PressEditorState
        val publishingId = controls[4] as Long?

        val statusBanner = feedValues[0] as String?
        val errorBanner = feedValues[1] as String?
        val archiveSource = feedValues[2] as TextArchiveSource
        @Suppress("UNCHECKED_CAST")
        val eitaaPosts = feedValues[3] as List<EitaaChannelPost>
        val isLoadingEitaa = feedValues[4] as Boolean
        val eitaaError = feedValues[5] as String?
        val logFilter = feedValues[6] as LogLevelFilter

        val cleanQuery = query.trim()
        val filteredArticles = articles.filter { article ->
            val matchesFilter = when (filter) {
                PressFilter.ALL -> true
                PressFilter.DRAFTS -> !article.isPublished
                PressFilter.PUBLISHED -> article.isPublished
                PressFilter.PINNED -> article.isPinned
            }
            val matchesQuery = cleanQuery.isEmpty() ||
                article.title.contains(cleanQuery, ignoreCase = true) ||
                article.body.contains(cleanQuery, ignoreCase = true) ||
                article.category.contains(cleanQuery, ignoreCase = true) ||
                article.tags.contains(cleanQuery, ignoreCase = true)
            matchesFilter && matchesQuery
        }

        val filteredLogs = logs.filter { log ->
            when (logFilter) {
                LogLevelFilter.ALL -> true
                LogLevelFilter.SUCCESS -> log.level == "SUCCESS"
                LogLevelFilter.ERROR -> log.level == "ERROR"
            }
        }

        PressUiState(
            selectedTab = tab,
            articles = filteredArticles,
            allArticles = articles,
            searchQuery = query,
            selectedFilter = filter,
            editor = editor,
            settings = settings,
            publishingArticleId = publishingId,
            statusBanner = statusBanner,
            errorBanner = errorBanner,
            archiveSource = archiveSource,
            eitaaPosts = eitaaPosts,
            isLoadingEitaaPosts = isLoadingEitaa,
            eitaaFetchError = eitaaError,
            logs = filteredLogs,
            logFilter = logFilter
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PressUiState()
    )

    fun selectTab(tab: PressSectionTab) {
        tabFlow.value = tab
        if (tab == PressSectionTab.TEXT_ARCHIVE &&
            archiveSourceFlow.value == TextArchiveSource.EITAA_CHANNEL &&
            eitaaPostsFlow.value.isEmpty()
        ) {
            refreshEitaaChannelPosts()
        }
    }

    fun selectArchiveSource(source: TextArchiveSource) {
        archiveSourceFlow.value = source
        if (source == TextArchiveSource.EITAA_CHANNEL && eitaaPostsFlow.value.isEmpty()) {
            refreshEitaaChannelPosts()
        }
    }

    fun selectLogFilter(filter: LogLevelFilter) {
        logFilterFlow.value = filter
    }

    fun dismissBanners() {
        statusBannerFlow.value = null
        errorBannerFlow.value = null
    }

    fun updateSearch(query: String) {
        searchFlow.value = query
    }

    fun selectFilter(filter: PressFilter) {
        filterFlow.value = filter
    }

    fun openNewEditor() {
        editorFlow.value = PressEditorState(isOpen = true)
    }

    fun openEditEditor(article: PressArticleEntity) {
        editorFlow.value = PressEditorState(
            isOpen = true,
            editingId = article.id,
            title = article.title,
            body = article.body,
            category = article.category,
            tags = article.tags,
            isPublished = article.isPublished
        )
    }

    fun closeEditor() {
        editorFlow.value = PressEditorState(isOpen = false)
    }

    fun updateEditorTitle(value: String) {
        editorFlow.value = editorFlow.value.copy(title = value)
    }

    fun updateEditorBody(value: String) {
        editorFlow.value = editorFlow.value.copy(body = value)
    }

    fun updateEditorCategory(value: String) {
        editorFlow.value = editorFlow.value.copy(category = value)
    }

    fun updateEditorTags(value: String) {
        editorFlow.value = editorFlow.value.copy(tags = value)
    }

    fun updateEditorPublished(value: Boolean) {
        editorFlow.value = editorFlow.value.copy(isPublished = value)
    }

    fun saveEditorArticle() {
        val current = editorFlow.value
        val title = current.title.trim()
        val body = current.body.trim()
        if (title.isEmpty() && body.isEmpty()) return

        viewModelScope.launch {
            val existing = if (current.editingId != 0L) {
                pressRepository.getArticleById(current.editingId)
            } else {
                null
            }
            val finalTitle = title.ifEmpty { "بدون عنوان" }
            val entity = PressArticleEntity(
                id = current.editingId,
                title = finalTitle,
                body = body,
                category = current.category.ifBlank { "فنی" },
                tags = current.tags.trim(),
                isPublished = current.isPublished,
                isPinned = existing?.isPinned ?: false,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                eitaaMessageId = existing?.eitaaMessageId,
                eitaaPublishedAt = existing?.eitaaPublishedAt
            )
            pressRepository.saveArticle(entity)
            logRepository.log(
                module = "Yasin Press",
                level = "INFO",
                title = if (current.editingId == 0L) "ثبت نوشته جدید" else "ویرایش نوشته",
                details = "عنوان: $finalTitle | وضعیت: ${if (entity.isPublished) "منتشرشده" else "پیش‌نویس"}"
            )
            closeEditor()
        }
    }

    fun publishToEitaa(article: PressArticleEntity) {
        if (publishingIdFlow.value != null) return
        viewModelScope.launch {
            dismissBanners()
            val settings = relayConfigRepository.settingsFlow.first()
            if (settings.eitaayarToken.isBlank() || settings.eitaaChannelId.isBlank()) {
                val err = "برای انتشار در ایتا ابتدا باید «کد ایتایار» و «آیدی کانال» را در بخش تنظیمات وارد کنید."
                errorBannerFlow.value = err
                logRepository.log(
                    module = "Eitaayar",
                    level = "ERROR",
                    title = "انتشار در ایتا ناموفق (تنظیمات ناقص)",
                    details = "نوشته: ${article.title} | $err"
                )
                return@launch
            }

            publishingIdFlow.value = article.id
            try {
                val result = eitaaRepository.publishToEitaa(
                    token = settings.eitaayarToken,
                    channelId = settings.eitaaChannelId,
                    title = article.title,
                    body = article.body
                )
                if (result.success) {
                    val now = System.currentTimeMillis()
                    pressRepository.saveArticle(
                        article.copy(
                            isPublished = true,
                            eitaaMessageId = result.messageId,
                            eitaaPublishedAt = now,
                            updatedAt = now
                        )
                    )
                    statusBannerFlow.value = result.detailFa
                    logRepository.log(
                        module = "Eitaayar",
                        level = "SUCCESS",
                        title = "انتشار موفق در کانال ایتا",
                        details = "نوشته: ${article.title} | کانال: ${settings.eitaaChannelId} | ${result.detailFa}"
                    )
                } else {
                    errorBannerFlow.value = result.detailFa
                    logRepository.log(
                        module = "Eitaayar",
                        level = "ERROR",
                        title = "خطا در انتشار ایتایار",
                        details = "نوشته: ${article.title} | کد: ${result.statusCode ?: "-"} | ${result.detailFa}"
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val err = e.localizedMessage ?: "خطای غیرمنتظره هنگام ارسال به ایتا."
                errorBannerFlow.value = err
                logRepository.log(
                    module = "Eitaayar",
                    level = "ERROR",
                    title = "استثنای ارسال به ایتا",
                    details = "نوشته: ${article.title} | $err"
                )
            } finally {
                publishingIdFlow.value = null
            }
        }
    }

    fun refreshEitaaChannelPosts() {
        if (isLoadingEitaaFlow.value) return
        viewModelScope.launch {
            isLoadingEitaaFlow.value = true
            eitaaErrorFlow.value = null
            val settings = relayConfigRepository.settingsFlow.first()
            val channel = settings.eitaaChannelId
            if (channel.isBlank()) {
                val msg = "آیدی کانال ایتا در تنظیمات وارد نشده است. روی «تنظیم ایتایار» بزنید و آیدی کانال را ثبت کنید."
                eitaaErrorFlow.value = msg
                isLoadingEitaaFlow.value = false
                return@launch
            }
            try {
                val posts = eitaaRepository.fetchChannelPosts(channel)
                eitaaPostsFlow.value = posts
                logRepository.log(
                    module = "Eitaayar",
                    level = "SUCCESS",
                    title = "دریافت پست‌های کانال عمومی ایتا",
                    details = "کانال: $channel | تعداد پست: ${posts.size}"
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val err = e.localizedMessage ?: "خطا در دریافت پست‌های کانال ایتا."
                eitaaErrorFlow.value = err
                logRepository.log(
                    module = "Eitaayar",
                    level = "ERROR",
                    title = "خطا در خواندن کانال ایتا",
                    details = "کانال: $channel | $err"
                )
            } finally {
                isLoadingEitaaFlow.value = false
            }
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            logRepository.clearLogs()
        }
    }

    fun assistBodyWithAi(instruction: String) {
        val current = editorFlow.value
        if (current.isAiAssisting) return

        viewModelScope.launch {
            editorFlow.value = editorFlow.value.copy(isAiAssisting = true, aiAssistError = null)
            try {
                val generated = aiGateway.generatePressAssist(
                    instruction = instruction,
                    currentTitle = current.title,
                    currentBody = current.body
                )
                editorFlow.value = editorFlow.value.copy(
                    body = generated,
                    isAiAssisting = false,
                    aiAssistError = null
                )
                logRepository.log(
                    module = "Yasin AI",
                    level = "SUCCESS",
                    title = "تکمیل متن در یاسین پرس",
                    details = "عنوان: ${current.title.ifBlank { "پیش‌نویس" }}"
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI Press assist timed out", e)
                editorFlow.value = editorFlow.value.copy(
                    isAiAssisting = false,
                    aiAssistError = "زمان تکمیل متن با هوش مصنوعی به پایان رسید."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Press assist failed", e)
                editorFlow.value = editorFlow.value.copy(
                    isAiAssisting = false,
                    aiAssistError = e.localizedMessage ?: "خطا در تولید متن با یاسین هوش."
                )
            }
        }
    }

    fun togglePin(article: PressArticleEntity) {
        viewModelScope.launch {
            pressRepository.togglePin(article)
        }
    }

    fun deleteArticle(id: Long) {
        viewModelScope.launch {
            val existing = pressRepository.getArticleById(id)
            pressRepository.deleteArticleById(id)
            if (existing != null) {
                logRepository.log(
                    module = "Yasin Press",
                    level = "INFO",
                    title = "حذف نوشته",
                    details = "عنوان: ${existing.title}"
                )
            }
        }
    }

    companion object {
        fun factory(
            pressRepository: PressRepository,
            aiGateway: AiGateway,
            eitaaRepository: EitaaRepository,
            relayConfigRepository: RelayConfigRepository,
            logRepository: LogRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PressViewModel(
                    pressRepository = pressRepository,
                    aiGateway = aiGateway,
                    eitaaRepository = eitaaRepository,
                    relayConfigRepository = relayConfigRepository,
                    logRepository = logRepository
                ) as T
            }
        }
    }
}
