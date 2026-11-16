package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class RelayPingResult(
    val reachable: Boolean,
    val statusCode: Int?,
    val latencyMs: Long,
    val detailFa: String
)

class RelayPingClient(private val okHttpClient: OkHttpClient) {
    suspend fun ping(rawUrl: String): RelayPingResult = withContext(Dispatchers.IO) {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) {
            return@withContext RelayPingResult(
                reachable = false,
                statusCode = null,
                latencyMs = 0L,
                detailFa = "آدرس سرور ریموت وارد نشده است."
            )
        }
        val normalized = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }

        val start = System.currentTimeMillis()
        try {
            val request = Request.Builder()
                .url(normalized)
                .get()
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                val elapsed = System.currentTimeMillis() - start
                RelayPingResult(
                    reachable = response.isSuccessful || response.code in 200..499,
                    statusCode = response.code,
                    latencyMs = elapsed,
                    detailFa = "پاسخ سرور با کد ${response.code} در $elapsed میلی‌ثانیه دریافت شد."
                )
            }
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - start
            RelayPingResult(
                reachable = false,
                statusCode = null,
                latencyMs = elapsed,
                detailFa = e.localizedMessage ?: "خطا در برقراری ارتباط با سرور ریموت."
            )
        }
    }
}
