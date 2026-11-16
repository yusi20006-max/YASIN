package com.example.ui.screens.docs

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.EcosystemInspectorRepository
import com.example.data.repository.RelayConfigRepository
import com.example.data.repository.RepoSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "DocsMcpViewModel"

sealed interface DocsLoadState {
    data object Loading : DocsLoadState
    data class Success(val repos: List<RepoSummary>) : DocsLoadState
    data class Error(val message: String) : DocsLoadState
}

data class ReadmeSheetState(
    val repo: RepoSummary? = null,
    val isLoading: Boolean = false,
    val content: String = "",
    val error: String? = null
)

data class DocsMcpUiState(
    val owner: String = "yusi20006-max",
    val searchQuery: String = "",
    val loadState: DocsLoadState = DocsLoadState.Loading,
    val readmeSheet: ReadmeSheetState = ReadmeSheetState()
)

class DocsMcpViewModel(
    private val inspectorRepository: EcosystemInspectorRepository,
    private val relayConfigRepository: RelayConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocsMcpUiState())
    val uiState: StateFlow<DocsMcpUiState> = _uiState.asStateFlow()

    init {
        refreshRepos()
    }

    fun updateSearch(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun refreshRepos() {
        viewModelScope.launch {
            val settings = relayConfigRepository.settingsFlow.first()
            val owner = settings.githubOwner.ifBlank { "yusi20006-max" }
            _uiState.value = _uiState.value.copy(
                owner = owner,
                loadState = DocsLoadState.Loading
            )
            try {
                val repos = inspectorRepository.fetchRepos(owner)
                _uiState.value = _uiState.value.copy(
                    owner = owner,
                    loadState = DocsLoadState.Success(repos)
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch GitHub repos", e)
                _uiState.value = _uiState.value.copy(
                    owner = owner,
                    loadState = DocsLoadState.Error(
                        e.localizedMessage ?: "خطا در دریافت لیست مخازن از گیت‌هاب."
                    )
                )
            }
        }
    }

    fun openRepoReadme(repo: RepoSummary) {
        viewModelScope.launch {
            val owner = _uiState.value.owner
            _uiState.value = _uiState.value.copy(
                readmeSheet = ReadmeSheetState(repo = repo, isLoading = true)
            )
            try {
                val readmeText = inspectorRepository.fetchReadme(owner, repo.name)
                _uiState.value = _uiState.value.copy(
                    readmeSheet = ReadmeSheetState(
                        repo = repo,
                        isLoading = false,
                        content = readmeText
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch README", e)
                _uiState.value = _uiState.value.copy(
                    readmeSheet = ReadmeSheetState(
                        repo = repo,
                        isLoading = false,
                        error = e.localizedMessage ?: "دریافت README ناموفق بود."
                    )
                )
            }
        }
    }

    fun closeReadmeSheet() {
        _uiState.value = _uiState.value.copy(readmeSheet = ReadmeSheetState())
    }

    companion object {
        fun factory(
            inspectorRepository: EcosystemInspectorRepository,
            relayConfigRepository: RelayConfigRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DocsMcpViewModel(inspectorRepository, relayConfigRepository) as T
            }
        }
    }
}
