package com.example.data.repository

import android.util.Log
import com.example.data.local.ChatMessageEntity
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "FirebaseAiLogicGateway"
private const val MODEL_NAME = "gemini-3.8-flash"
private const val AI_TIMEOUT_MS = 60_000L

class FirebaseAiLogicGateway : AiGateway {

    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME
        )
    }

    private val agentModel by lazy {
        val agentSchema = Schema.obj(
            properties = mapOf(
                "actionType" to Schema.enumeration(
                    values = listOf("CREATE_PRESS_DRAFT", "INSPECT_REPO", "ANSWER_ONLY"),
                    description = "The native action to execute inside YasinHub"
                ),
                "replyFa" to Schema.string(
                    description = "Clear Persian explanation for the user describing what the agent is doing or answering"
                ),
                "articleTitle" to Schema.string(
                    description = "Persian article title when actionType is CREATE_PRESS_DRAFT",
                    nullable = true
                ),
                "articleCategory" to Schema.string(
                    description = "Persian category such as فنی, معماری, یادداشت, گزارش",
                    nullable = true
                ),
                "articleBody" to Schema.string(
                    description = "Well-structured Persian article content when actionType is CREATE_PRESS_DRAFT",
                    nullable = true
                ),
                "targetRepo" to Schema.string(
                    description = "Exact repository name (e.g. Openfeed, DigiSnap-MCP, Yasin-MCP, YASIN-DOCS) when actionType is INSPECT_REPO",
                    nullable = true
                )
            ),
            optionalProperties = listOf("articleTitle", "articleCategory", "articleBody", "targetRepo")
        )

        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = agentSchema
            }
        )
    }

    override fun streamChatReply(
        history: List<ChatMessageEntity>,
        prompt: String,
        systemInstruction: String
    ): Flow<String> = flow {
        try {
            val recentHistory: List<Content> = history
                .filter { it.role == "user" || it.role == "model" }
                .takeLast(10)
                .map { msg ->
                    content(role = msg.role) {
                        text(msg.content)
                    }
                }

            val fullPrompt = if (recentHistory.isEmpty()) {
                "$systemInstruction\n\nدرخواست کاربر:\n$prompt"
            } else {
                prompt
            }

            val chat = chatModel.startChat(history = recentHistory)
            val accumulated = StringBuilder()

            withTimeout(AI_TIMEOUT_MS) {
                chat.sendMessageStream(fullPrompt).collect { chunk ->
                    val part = chunk.text.orEmpty()
                    if (part.isNotEmpty()) {
                        accumulated.append(part)
                        emit(accumulated.toString())
                    }
                }
            }

            if (accumulated.isBlank()) {
                throw IllegalStateException("مدل هوش مصنوعی پاسخی برنگرداند.")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "streamChatReply failed", e)
            throw e
        }
    }

    override suspend fun generatePressAssist(
        instruction: String,
        currentTitle: String,
        currentBody: String
    ): String {
        return try {
            val prompt = buildString {
                appendLine("تو دستیار تحریریه ماژول یاسین پرس (Yasin Press) هستی. همیشه به زبان فارسی روان، دقیق و حرفه‌ای بنویس.")
                appendLine("دستور کاربر: $instruction")
                if (currentTitle.isNotBlank()) {
                    appendLine("عنوان فعلی: $currentTitle")
                }
                if (currentBody.isNotBlank()) {
                    appendLine("متن فعلی:\n$currentBody")
                }
                appendLine("فقط متن پیشنهادی نهایی را برگردان تا مستقیم در ویرایشگر قرار گیرد.")
            }

            val response = withTimeout(AI_TIMEOUT_MS) {
                chatModel.generateContent(prompt)
            }
            response.text?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("مدل پاسخی تولید نکرد.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "generatePressAssist failed", e)
            throw e
        }
    }

    override suspend fun planAgentAction(
        goal: String,
        ecosystemContext: String
    ): AgentActionPlan {
        return try {
            val prompt = buildString {
                appendLine("تو Yasin Agent در اپلیکیشن اندروید یاسین‌هاب هستی. وظیفه تو تحلیل هدف کاربر و انتخاب بهترین اکشن بومی است.")
                appendLine("قوانین:")
                appendLine("1. اگر کاربر خواست یادداشت، مقاله، گزارش یا پیش‌نویس بنویسی یا در یاسین پرس ذخیره کنی، actionType را CREATE_PRESS_DRAFT بگذار و فیلدهای articleTitle، articleCategory و articleBody را کامل به فارسی پر کن.")
                appendLine("2. اگر کاربر خواست یک مخزن گیت‌هاب یا مستنداتش را بررسی/بازرسی کنی، actionType را INSPECT_REPO بگذار و targetRepo را مشخص کن.")
                appendLine("3. در غیر این صورت actionType را ANSWER_ONLY بگذار و پاسخ جامع فارسی در replyFa بده.")
                appendLine("\nوضعیت فعلی اکوسیستم:\n$ecosystemContext")
                appendLine("\nهدف کاربر:\n$goal")
            }

            val response = withTimeout(AI_TIMEOUT_MS) {
                agentModel.generateContent(prompt)
            }
            val jsonString = response.text ?: throw IllegalStateException("پاسخ ساختاریافته ایجنت خالی بود.")
            val json = JSONObject(jsonString)

            AgentActionPlan(
                actionType = json.optString("actionType", "ANSWER_ONLY"),
                replyFa = json.optString("replyFa", "عملیات بررسی شد."),
                articleTitle = json.optString("articleTitle", ""),
                articleCategory = json.optString("articleCategory", "فنی").ifBlank { "فنی" },
                articleBody = json.optString("articleBody", ""),
                targetRepo = json.optString("targetRepo", "")
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "planAgentAction failed", e)
            throw e
        }
    }
}
