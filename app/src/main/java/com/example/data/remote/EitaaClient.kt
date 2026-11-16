package com.example.data.remote

import androidx.core.text.HtmlCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class EitaaPublishResult(
    val success: Boolean,
    val messageId: String?,
    val statusCode: Int?,
    val detailFa: String,
    val rawResponse: String
)

data class EitaaChannelPost(
    val postId: String,
    val dateText: String,
    val text: String
)

class EitaaClient(private val okHttpClient: OkHttpClient) {

    suspend fun sendMessage(
        token: String,
        chatId: String,
        text: String,
        title: String? = null
    ): EitaaPublishResult = withContext(Dispatchers.IO) {
        val cleanToken = token.trim()
        val cleanChatId = normalizeChannelId(chatId)
        if (cleanToken.isEmpty() || cleanChatId.isEmpty()) {
            return@withContext EitaaPublishResult(
                success = false,
                messageId = null,
                statusCode = null,
                detailFa = "کد ایتایار (Token) یا آیدی کانال مقصد وارد نشده است.",
                rawResponse = ""
            )
        }

        val formBuilder = FormBody.Builder()
            .add("chat_id", cleanChatId)
            .add("text", text)
        if (!title.isNullOrBlank()) {
            formBuilder.add("title", title)
        }

        val endpoint = "https://eitaayar.ir/api/$cleanToken/sendMessage"
        val request = Request.Builder()
            .url(endpoint)
            .post(formBuilder.build())
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext EitaaPublishResult(
                        success = false,
                        messageId = null,
                        statusCode = response.code,
                        detailFa = "خطای HTTP ${response.code} از سرور ایتایار.",
                        rawResponse = rawBody
                    )
                }

                val json = try {
                    JSONObject(rawBody)
                } catch (_: Exception) {
                    null
                }

                val ok = json?.optBoolean("ok", false) ?: false
                val resultObj = json?.optJSONObject("result")
                val msgId = resultObj?.opt("message_id")?.toString()
                val description = json?.optString("description").orEmpty()

                if (ok) {
                    EitaaPublishResult(
                        success = true,
                        messageId = msgId,
                        statusCode = response.code,
                        detailFa = if (!msgId.isNullOrBlank()) {
                            "پیام با موفقیت در ایتا منتشر شد (شناسه پیام: $msgId)."
                        } else {
                            "پیام با موفقیت در ایتا منتشر شد."
                        },
                        rawResponse = rawBody
                    )
                } else {
                    EitaaPublishResult(
                        success = false,
                        messageId = null,
                        statusCode = response.code,
                        detailFa = description.ifBlank { "ایتایار ارسال پیام را تأیید نکرد." },
                        rawResponse = rawBody
                    )
                }
            }
        } catch (e: Exception) {
            EitaaPublishResult(
                success = false,
                messageId = null,
                statusCode = null,
                detailFa = e.localizedMessage ?: "خطا در اتصال به سرور ایتایار.",
                rawResponse = ""
            )
        }
    }

    suspend fun fetchPublicChannelPosts(channelId: String): List<EitaaChannelPost> =
        withContext(Dispatchers.IO) {
            val cleanChannel = normalizeChannelId(channelId)
            if (cleanChannel.isEmpty()) {
                throw IllegalArgumentException("آیدی کانال ایتا در تنظیمات وارد نشده است.")
            }

            val url = "https://eitaa.com/$cleanChannel"
            val request = Request.Builder()
                .url(url)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36"
                )
                .get()
                .build()

            val html = okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("خطا در دریافت صفحه کانال ایتا (کد ${response.code})")
                }
                response.body?.string().orEmpty()
            }

            parseEitaaChannelHtml(html)
        }

    private fun normalizeChannelId(raw: String): String {
        return raw.trim()
            .removePrefix("https://eitaa.com/")
            .removePrefix("http://eitaa.com/")
            .removePrefix("eitaa.com/")
            .removePrefix("@")
            .trim('/')
    }

    private fun parseEitaaChannelHtml(html: String): List<EitaaChannelPost> {
        if (html.isBlank()) return emptyList()

        // Split around each message wrapper in Eitaa web preview
        val chunks = html.split("etme_widget_message_wrap")
        if (chunks.size <= 1) return emptyList()

        val textRegex = Regex(
            """<div[^>]*class="[^"]*etme_widget_message_text[^"]*"[^>]*>(.*?)</div>""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        )
        val dateRegex = Regex(
            """datetime="([^"]+)"""",
            setOf(RegexOption.IGNORE_CASE)
        )
        val postIdRegex = Regex(
            """data-post="([^"]+)"""",
            setOf(RegexOption.IGNORE_CASE)
        )

        val posts = mutableListOf<EitaaChannelPost>()
        for (chunk in chunks.drop(1)) {
            val textHtml = textRegex.find(chunk)?.groupValues?.get(1) ?: continue
            val cleanText = HtmlCompat.fromHtml(
                textHtml.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n"),
                HtmlCompat.FROM_HTML_MODE_COMPACT
            ).toString().trim()

            if (cleanText.isBlank()) continue

            val postId = postIdRegex.find(chunk)?.groupValues?.get(1).orEmpty()
            val date = dateRegex.find(chunk)?.groupValues?.get(1)?.take(16)?.replace("T", " ").orEmpty()

            posts.add(
                EitaaChannelPost(
                    postId = postId.ifBlank { "${posts.size + 1}" },
                    dateText = date,
                    text = cleanText
                )
            )
        }
        return posts.reversed()
    }
}
