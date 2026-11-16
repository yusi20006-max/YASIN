package com.example.ui.screens.relay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.remote.RelayPingResult
import com.example.data.repository.EitaaRepository
import com.example.data.repository.LogRepository
import com.example.data.repository.RelayConfigRepository
import com.example.data.repository.RelaySettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EitaaTestResult(
    val success: Boolean,
    val messageFa: String
)

data class RelaySettingsUiState(
    val settings: RelaySettings = RelaySettings(),
    val isSettingsLoaded: Boolean = false,
    val isPinging: Boolean = false,
    val pingResult: RelayPingResult? = null,
    val isTestingEitaa: Boolean = false,
    val eitaaTestResult: EitaaTestResult? = null,
    val savedBanner: String? = null
)

class RelaySettingsViewModel(
    private val relayConfigRepository: RelayConfigRepository,
    private val eitaaRepository: EitaaRepository,
    private val logRepository: LogRepository
) : ViewModel() {

    private val isPingingFlow = MutableStateFlow(false)
    private val pingResultFlow = MutableStateFlow<RelayPingResult?>(null)
    private val isTestingEitaaFlow = MutableStateFlow(false)
    private val eitaaTestResultFlow = MutableStateFlow<EitaaTestResult?>(null)
    private val savedBannerFlow = MutableStateFlow<String?>(null)

    private val loadedSettingsFlow = relayConfigRepository.settingsFlow.map { settings ->
        settings to true
    }

    val uiState: StateFlow<RelaySettingsUiState> = combine(
        loadedSettingsFlow,
        isPingingFlow,
        pingResultFlow,
        isTestingEitaaFlow,
        eitaaTestResultFlow,
        savedBannerFlow
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val loadedPair = values[0] as Pair<RelaySettings, Boolean>
        val pinging = values[1] as Boolean
        val pingResult = values[2] as RelayPingResult?
        val testingEitaa = values[3] as Boolean
        val eitaaResult = values[4] as EitaaTestResult?
        val saved = values[5] as String?

        RelaySettingsUiState(
            settings = loadedPair.first,
            isSettingsLoaded = loadedPair.second,
            isPinging = pinging,
            pingResult = pingResult,
            isTestingEitaa = testingEitaa,
            eitaaTestResult = eitaaResult,
            savedBanner = saved
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RelaySettingsUiState()
    )

    fun saveEitaayarSettings(token: String, channelId: String) {
        viewModelScope.launch {
            relayConfigRepository.updateEitaayarConfig(token, channelId)
            val cleanChannel = channelId.trim().ifBlank { "تعریف‌نشده" }
            logRepository.log(
                module = "Eitaayar",
                level = "INFO",
                title = "ذخیره تنظیمات ایتایار در DataStore",
                details = "کانال مقصد: $cleanChannel | وضعیت کلید API: ${if (token.isNotBlank()) "ثبت شد" else "خالی"}"
            )
            savedBannerFlow.value = "کد ایتایار و شناسه کانال با موفقیت در DataStore ذخیره شدند."
        }
    }

    fun clearEitaayarToken() {
        viewModelScope.launch {
            val currentChannel = uiState.value.settings.eitaaChannelId
            relayConfigRepository.updateEitaayarConfig("", currentChannel)
            logRepository.log(
                module = "Eitaayar",
                level = "INFO",
                title = "پاک کردن کلید ایتایار",
                details = "کلید Eitaa-yar API از DataStore حذف شد."
            )
            savedBannerFlow.value = "کد ایتایار از حافظه تنظیمات (DataStore) پاک شد."
        }
    }

    fun testEitaaChannel(channelId: String) {
        if (isTestingEitaaFlow.value) return
        viewModelScope.launch {
            isTestingEitaaFlow.value = true
            eitaaTestResultFlow.value = null
            try {
                val posts = eitaaRepository.fetchChannelPosts(channelId)
                val msg = "اتصال به کانال برقرار شد (${posts.size} پست عمومی قابل مشاهده است)."
                eitaaTestResultFlow.value = EitaaTestResult(true, msg)
                logRepository.log(
                    module = "Eitaayar",
                    level = "SUCCESS",
                    title = "بررسی کانال عمومی ایتا",
                    details = "کانال: $channelId | تعداد پست دریافتی: ${posts.size}"
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val err = e.localizedMessage ?: "خطا در بررسی کانال ایتا."
                eitaaTestResultFlow.value = EitaaTestResult(false, err)
                logRepository.log(
                    module = "Eitaayar",
                    level = "ERROR",
                    title = "خطا در بررسی کانال ایتا",
                    details = "کانال: $channelId | خطا: $err"
                )
            } finally {
                isTestingEitaaFlow.value = false
            }
        }
    }

    fun saveSettings(relayUrl: String, githubOwner: String) {
        viewModelScope.launch {
            relayConfigRepository.updateRelayUrl(relayUrl)
            relayConfigRepository.updateGithubOwner(githubOwner)
            logRepository.log(
                module = "Yasin Relay",
                level = "INFO",
                title = "به‌روزرسانی تنظیمات ریلی و گیت‌هاب",
                details = "GitHub: $githubOwner | Relay: ${relayUrl.ifBlank { "غیرفعال" }}"
            )
            savedBannerFlow.value = "تنظیمات پل ریموت و گیت‌هاب ذخیره شد."
        }
    }

    fun dismissSavedBanner() {
        savedBannerFlow.value = null
    }

    fun toggleDarkTheme(dark: Boolean) {
        viewModelScope.launch {
            relayConfigRepository.updateDarkTheme(dark)
        }
    }

    fun toggleAutoContext(enabled: Boolean) {
        viewModelScope.launch {
            relayConfigRepository.updateAutoAttachContext(enabled)
        }
    }

    fun testConnection(url: String) {
        if (isPingingFlow.value) return
        viewModelScope.launch {
            isPingingFlow.value = true
            pingResultFlow.value = null
            val result = relayConfigRepository.pingRelay(url)
            pingResultFlow.value = result
            logRepository.log(
                module = "Yasin Relay",
                level = if (result.reachable) "SUCCESS" else "ERROR",
                title = if (result.reachable) "تست موفق اتصال سرور ریموت" else "خطا در اتصال به سرور ریموت",
                details = result.detailFa
            )
            isPingingFlow.value = false
        }
    }

    companion object {
        fun factory(
            relayConfigRepository: RelayConfigRepository,
            eitaaRepository: EitaaRepository,
            logRepository: LogRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RelaySettingsViewModel(
                    relayConfigRepository = relayConfigRepository,
                    eitaaRepository = eitaaRepository,
                    logRepository = logRepository
                ) as T
            }
        }
    }
}
