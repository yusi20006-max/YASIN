package com.example.data.repository

import com.example.data.local.PressArticleEntity
import com.example.data.local.TelegramChannelEntity
import com.example.data.local.YasinDao
import com.example.data.local.YasinDatabase
import com.example.data.remote.TelegramChannelClient
import com.example.data.remote.TelegramChannelPost
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

data class PipelineRunResult(
    val fetchedCount: Int,
    val savedCount: Int,
    val publishedToEitaaCount: Int,
    val failedChannels: List<String>,
    val summaryFa: String,
    val hasErrors: Boolean
)

class RoomTelegramChannelRepository(
    private val dao: YasinDao
) : TelegramChannelRepository {

    override fun observeChannels(): Flow<List<TelegramChannelEntity>> =
        dao.observeTelegramChannels()

    override suspend fun ensureDefaultsSeeded() {
        dao.insertDefaultTelegramChannels(YasinDatabase.defaultTelegramChannels)
    }

    override suspend fun addChannel(
        rawUsername: String,
        titleFa: String
    ): Result<TelegramChannelEntity> {
        val cleanUsername = TelegramChannelClient.normalizeUsername(rawUsername)
        if (cleanUsername.length < 3 || !cleanUsername.matches(Regex("^[a-z0-9_]{3,64}$"))) {
            return Result.failure(
                IllegalArgumentException("نام کاربری کانال معتبر نیست (مثلاً zoomit یا @khabarfouri).")
            )
        }
        val cleanTitle = titleFa.trim().ifBlank { "@$cleanUsername" }
        val entity = TelegramChannelEntity(
            username = cleanUsername,
            titleFa = cleanTitle,
            isDefault = false,
            isEnabled = true
        )
        dao.upsertTelegramChannel(entity)
        return Result.success(entity)
    }

    override suspend fun toggleChannel(channel: TelegramChannelEntity) {
        dao.upsertTelegramChannel(channel.copy(isEnabled = !channel.isEnabled))
    }

    override suspend fun deleteChannel(username: String) {
        dao.deleteTelegramChannel(username)
    }

    override suspend fun restoreDefaults() {
        dao.insertDefaultTelegramChannels(YasinDatabase.defaultTelegramChannels)
    }
}

class TelegramPressPipeline(
    private val dao: YasinDao,
    private val telegramClient: TelegramChannelClient,
    private val pressRepository: PressRepository,
    private val eitaaRepository: EitaaRepository,
    private val relayConfigRepository: RelayConfigRepository,
    private val aiGateway: AiGateway,
    private val logRepository: LogRepository
) {
    suspend fun getPublishedLastHourCount(): Int {
        val oneHourAgo = System.currentTimeMillis() - 3_600_000L
        return dao.countAutoPublishedSince(oneHourAgo)
    }

    suspend fun runCollectionAndPublish(triggerSource: String): PipelineRunResult {
        dao.insertDefaultTelegramChannels(YasinDatabase.defaultTelegramChannels)
        val settings = relayConfigRepository.settingsFlow.first()
        val enabledChannels = dao.getEnabledTelegramChannels()

        if (enabledChannels.isEmpty()) {
            val msg = "هیچ کانال تلگرام فعالی انتخاب نشده است. حداقل یک کانال را فعال کنید."
            logRepository.log(
                module = "Yasin Press Auto",
                level = "ERROR",
                title = "توقف گردآوری خودکار ($triggerSource)",
                details = msg
            )
            return PipelineRunResult(0, 0, 0, emptyList(), msg, hasErrors = true)
        }

        val hourlyLimit = settings.pressHourlyLimit.coerceIn(1, 50)
        val publishedLastHour = getPublishedLastHourCount()
        val remainingQuota = (hourlyLimit - publishedLastHour).coerceAtLeast(0)

        if (remainingQuota == 0) {
            val msg = "سقف ساعتی ($hourlyLimit خبر در ساعت اخیر) تکمیل شده است. در ساعت بعدی به‌صورت خودکار ادامه می‌یابد."
            logRepository.log(
                module = "Yasin Press Auto",
                level = "INFO",
                title = "تکمیل سهمیه ساعتی ($triggerSource)",
                details = msg
            )
            return PipelineRunResult(0, 0, 0, emptyList(), msg, hasErrors = false)
        }

        val channelQueues = mutableListOf<MutableList<TelegramChannelPost>>()
        val failedChannels = mutableListOf<String>()
        var totalFetched = 0

        for (channel in enabledChannels) {
            try {
                val posts = telegramClient.fetchChannelPosts(
                    rawChannel = channel.username,
                    openFeedRelayBaseUrl = settings.relayBaseUrl
                )
                totalFetched += posts.size
                if (posts.isNotEmpty()) {
                    channelQueues.add(posts.toMutableList())
                }
            } catch (e: Exception) {
                failedChannels.add("@${channel.username}")
                logRepository.log(
                    module = "Telegram Fetch",
                    level = "ERROR",
                    title = "خطا در خواندن کانال @${channel.username}",
                    details = e.localizedMessage ?: "عدم دسترسی به پیش‌نمایش عمومی تلگرام یا سرور ریلی."
                )
            }
        }

        // Round-robin pick new unseen posts across enabled channels up to remainingQuota (default 20/hour)
        val candidates = mutableListOf<TelegramChannelPost>()
        val channelByUsername = enabledChannels.associateBy { it.username }

        var progress = true
        while (candidates.size < remainingQuota && progress) {
            progress = false
            for (queue in channelQueues) {
                while (queue.isNotEmpty()) {
                    val post = queue.removeAt(0)
                    if (dao.countArticlesBySourceKey(post.sourcePostKey) == 0) {
                        candidates.add(post)
                        progress = true
                        break
                    }
                }
                if (candidates.size >= remainingQuota) break
            }
        }

        val hasEitaaCreds = settings.eitaayarToken.isNotBlank() && settings.eitaaChannelId.isNotBlank()
        var savedCount = 0
        var publishedToEitaaCount = 0

        for ((index, post) in candidates.withIndex()) {
            val channelTitle = channelByUsername[post.channelUsername]?.titleFa ?: "@${post.channelUsername}"
            val rawTitle = extractHeadline(post.text)
            val finalBody = if (settings.pressAutoRewriteWithAi) {
                try {
                    aiGateway.generatePressAssist(
                        instruction = "این خبر تلگرامی را به یک خبر کوتاه، دقیق، حرفه‌ای و روان به فارسی برای انتشار در کانال خبری یاسین پرس بازنویسی کن و در انتها منبع را ذکر کن.",
                        currentTitle = rawTitle,
                        currentBody = post.text
                    )
                } catch (_: Exception) {
                    post.text
                }
            } else {
                post.text
            }

            val attributedBody = "$finalBody\n\n📡 منبع: $channelTitle (@${post.channelUsername})"
            val now = System.currentTimeMillis()
            val draft = PressArticleEntity(
                title = rawTitle,
                body = attributedBody,
                category = "تلگرام",
                tags = "خودکار,${post.channelUsername}",
                isPublished = false,
                createdAt = now,
                updatedAt = now,
                sourceChannel = post.channelUsername,
                sourcePostKey = post.sourcePostKey
            )
            val savedId = pressRepository.saveArticle(draft)
            savedCount++

            if (hasEitaaCreds) {
                val res = eitaaRepository.publishToEitaa(
                    token = settings.eitaayarToken,
                    channelId = settings.eitaaChannelId,
                    title = rawTitle,
                    body = attributedBody
                )
                if (res.success) {
                    val publishTime = System.currentTimeMillis()
                    pressRepository.saveArticle(
                        draft.copy(
                            id = savedId,
                            isPublished = true,
                            eitaaMessageId = res.messageId,
                            eitaaPublishedAt = publishTime,
                            updatedAt = publishTime
                        )
                    )
                    publishedToEitaaCount++
                } else {
                    logRepository.log(
                        module = "Eitaayar Auto",
                        level = "ERROR",
                        title = "خطا در انتشار خودکار خبر @${post.channelUsername}",
                        details = "عنوان: $rawTitle | ${res.detailFa}"
                    )
                }
                if (index < candidates.lastIndex) {
                    delay(1_100L)
                }
            }
        }

        relayConfigRepository.updatePressLastAutoRunAt(System.currentTimeMillis())

        val summary = buildString {
            append("اجرا ($triggerSource): ")
            append("بررسی ${enabledChannels.size} کانال")
            append(" • دریافت $totalFetched پیام")
            append(" • ذخیره $savedCount خبر جدید")
            if (hasEitaaCreds) {
                append(" • انتشار $publishedToEitaaCount مورد در ایتا (سقف $hourlyLimit/ساعت)")
            } else {
                append(" • ایتایار تنظیم نشده (خبرها به‌صورت آماده انتشار ذخیره شدند)")
            }
            if (failedChannels.isNotEmpty()) {
                append(" • ناموفق: ${failedChannels.joinToString("، ")}")
            }
        }

        logRepository.log(
            module = "Yasin Press Auto",
            level = if (failedChannels.size == enabledChannels.size && savedCount == 0) "ERROR" else "SUCCESS",
            title = "گزارش انتشار خودکار یاسین پرس",
            details = summary
        )

        return PipelineRunResult(
            fetchedCount = totalFetched,
            savedCount = savedCount,
            publishedToEitaaCount = publishedToEitaaCount,
            failedChannels = failedChannels,
            summaryFa = summary,
            hasErrors = failedChannels.size == enabledChannels.size && savedCount == 0
        )
    }

    private fun extractHeadline(text: String): String {
        val firstLine = text.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.length >= 6 }
            ?: text.trim()
        return if (firstLine.length <= 85) {
            firstLine
        } else {
            firstLine.take(82).trimEnd() + "..."
        }
    }
}
