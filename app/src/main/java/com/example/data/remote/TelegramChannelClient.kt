package com.example.data.remote

import androidx.core.text.HtmlCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

data class TelegramChannelPost(
    val channelUsername: String,
    val postId: String,
    val sourcePostKey: String,
    val dateText: String,
    val text: String
)

class TelegramChannelClient(private val okHttpClient: OkHttpClient) {

    suspend fun fetchChannelPosts(
        rawChannel: String,
        openFeedRelayBaseUrl: String = ""
    ): List<TelegramChannelPost> = withContext(Dispatchers.IO) {
        val cleanUsername = normalizeUsername(rawChannel)
        if (cleanUsername.isEmpty()) {
            throw IllegalArgumentException("نام کاربری کانال تلگرام نامعتبر است.")
        }

        // If user configured an Openfeed / Yasin Relay server URL, try its JSON API first
        val relay = openFeedRelayBaseUrl.trim().trimEnd('/')
        if (relay.startsWith("http://") || relay.startsWith("https://")) {
            try {
                val relayPosts = fetchFromOpenFeedRelay(relay, cleanUsername)
                if (relayPosts.isNotEmpty()) return@withContext relayPosts
            } catch (_: Exception) {
                // Fallback to direct public t.me/s/ page
            }
        }

        val url = "https://t.me/s/$cleanUsername"
        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36"
            )
            .header("Accept-Language", "fa-IR,fa;q=0.9,en;q=0.8")
            .get()
            .build()

        val html = okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("خطا در دریافت کانال تلگرام @$cleanUsername (کد HTTP ${response.code})")
            }
            response.body?.string().orEmpty()
        }

        parseTelegramPreviewHtml(cleanUsername, html)
    }

    private fun fetchFromOpenFeedRelay(
        relayBaseUrl: String,
        channelUsername: String
    ): List<TelegramChannelPost> {
        val request = Request.Builder()
            .url("$relayBaseUrl/api/channel/$channelUsername")
            .get()
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) return emptyList()

            val array = when {
                body.trimStart().startsWith("[") -> JSONArray(body)
                else -> JSONObject(body).optJSONArray("posts")
                    ?: JSONObject(body).optJSONArray("items")
                    ?: return emptyList()
            }

            val posts = mutableListOf<TelegramChannelPost>()
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val text = item.optString("text").ifBlank { item.optString("content") }.trim()
                if (text.isBlank()) continue
                val id = item.optString("id").ifBlank { "${i + 1}" }
                val date = item.optString("date").ifBlank { item.optString("published_at") }
                posts.add(
                    TelegramChannelPost(
                        channelUsername = channelUsername,
                        postId = id,
                        sourcePostKey = "$channelUsername/$id",
                        dateText = date,
                        text = text
                    )
                )
            }
            return posts
        }
    }

    private fun parseTelegramPreviewHtml(
        channelUsername: String,
        html: String
    ): List<TelegramChannelPost> {
        if (html.isBlank()) return emptyList()
        val chunks = html.split("tgme_widget_message_wrap")
        if (chunks.size <= 1) return emptyList()

        val postIdRegex = Regex("""data-post="([^"]+)"""", RegexOption.IGNORE_CASE)
        val dateRegex = Regex("""datetime="([^"]+)"""", RegexOption.IGNORE_CASE)
        val textBlockRegex = Regex(
            """<div[^>]*class="[^"]*tgme_widget_message_text[^"]*"[^>]*>(.*?)(?:<div[^>]*class="[^"]*tgme_widget_message_footer|</div>\s*<div[^>]*class="[^"]*tgme_widget_message_footer|</div>)""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        )

        val parsed = mutableListOf<TelegramChannelPost>()
        for (chunk in chunks.drop(1)) {
            val rawPostAttr = postIdRegex.find(chunk)?.groupValues?.get(1).orEmpty()
            val textHtml = textBlockRegex.find(chunk)?.groupValues?.get(1) ?: continue
            val normalizedHtml = textHtml
                .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
                .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n")

            val cleanText = HtmlCompat.fromHtml(
                normalizedHtml,
                HtmlCompat.FROM_HTML_MODE_COMPACT
            ).toString()
                .replace(Regex("\n{3,}"), "\n\n")
                .trim()

            if (cleanText.length < 12) continue

            val postId = rawPostAttr.substringAfter('/', rawPostAttr)
                .ifBlank { cleanText.hashCode().toString() }
            val sourceKey = "$channelUsername/$postId"
            val date = dateRegex.find(chunk)?.groupValues?.get(1)
                ?.take(16)
                ?.replace("T", " ")
                .orEmpty()

            parsed.add(
                TelegramChannelPost(
                    channelUsername = channelUsername,
                    postId = postId,
                    sourcePostKey = sourceKey,
                    dateText = date,
                    text = cleanText
                )
            )
        }
        // Return newest posts first
        return parsed.reversed()
    }

    companion object {
        fun normalizeUsername(raw: String): String {
            return raw.trim()
                .removePrefix("https://t.me/s/")
                .removePrefix("http://t.me/s/")
                .removePrefix("https://t.me/")
                .removePrefix("http://t.me/")
                .removePrefix("t.me/s/")
                .removePrefix("t.me/")
                .removePrefix("@")
                .substringBefore('/')
                .substringBefore('?')
                .trim()
                .lowercase()
        }
    }
}
