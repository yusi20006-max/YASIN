package com.example.ui.screens.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.registry.ModuleExecutionMode
import com.example.core.registry.YasinModuleDescriptor
import com.example.core.registry.YasinModuleRegistry
import com.example.data.repository.PressRepository
import com.example.data.repository.RelayConfigRepository
import com.example.data.repository.RelaySettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class HubModuleFilter(val labelFa: String) {
    ALL("همه (۹)"),
    NATIVE_ACTIVE("نیتیو فعال"),
    REMOTE_READY("آماده اتصال ریموت")
}

data class HubUiState(
    val modules: List<YasinModuleDescriptor> = YasinModuleRegistry.modules,
    val selectedFilter: HubModuleFilter = HubModuleFilter.ALL,
    val pressArticleCount: Int = 0,
    val nativeActiveCount: Int = YasinModuleRegistry.modules.count {
        it.executionMode == ModuleExecutionMode.NATIVE_ACTIVE
    },
    val remoteReadyCount: Int = YasinModuleRegistry.modules.count {
        it.executionMode == ModuleExecutionMode.REMOTE_EXTENSION_READY
    },
    val settings: RelaySettings = RelaySettings(),
    val selectedModuleForSheet: YasinModuleDescriptor? = null
)

class HubViewModel(
    pressRepository: PressRepository,
    relayConfigRepository: RelayConfigRepository
) : ViewModel() {

    private val filterFlow = MutableStateFlow(HubModuleFilter.ALL)
    private val selectedSheetModule = MutableStateFlow<YasinModuleDescriptor?>(null)

    val uiState: StateFlow<HubUiState> = combine(
        pressRepository.observeArticles(),
        relayConfigRepository.settingsFlow,
        filterFlow,
        selectedSheetModule
    ) { articles, settings, filter, sheetModule ->
        val filtered = when (filter) {
            HubModuleFilter.ALL -> YasinModuleRegistry.modules
            HubModuleFilter.NATIVE_ACTIVE -> YasinModuleRegistry.modules.filter {
                it.executionMode == ModuleExecutionMode.NATIVE_ACTIVE
            }
            HubModuleFilter.REMOTE_READY -> YasinModuleRegistry.modules.filter {
                it.executionMode == ModuleExecutionMode.REMOTE_EXTENSION_READY
            }
        }
        HubUiState(
            modules = filtered,
            selectedFilter = filter,
            pressArticleCount = articles.size,
            settings = settings,
            selectedModuleForSheet = sheetModule
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HubUiState()
    )

    fun selectFilter(filter: HubModuleFilter) {
        filterFlow.value = filter
    }

    fun openModuleDetails(module: YasinModuleDescriptor) {
        selectedSheetModule.value = module
    }

    fun dismissModuleDetails() {
        selectedSheetModule.value = null
    }

    companion object {
        fun factory(
            pressRepository: PressRepository,
            relayConfigRepository: RelayConfigRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HubViewModel(pressRepository, relayConfigRepository) as T
            }
        }
    }
}
