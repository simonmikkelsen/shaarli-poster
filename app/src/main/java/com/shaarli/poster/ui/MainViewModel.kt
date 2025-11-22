package com.shaarli.poster.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shaarli.poster.data.metadata.OkHttpTitleFetcher
import com.shaarli.poster.data.metadata.TitleFetcher
import com.shaarli.poster.data.model.Draft
import com.shaarli.poster.data.model.LinkPayload
import com.shaarli.poster.data.model.ShareFormState
import com.shaarli.poster.data.model.ShareStatus
import com.shaarli.poster.data.model.ShaarliSettings
import com.shaarli.poster.data.repository.PosterRepository
import com.shaarli.poster.data.repository.PostStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConnectionUiState(
    val status: ConnectionStatus = ConnectionStatus.Idle,
    val message: String? = null,
    val lastSuccessEpochMillis: Long? = null
)

enum class ConnectionStatus { Idle, Checking, Online, Offline, Error }

data class AppUiState(
    val settings: ShaarliSettings = ShaarliSettings(),
    val shareForm: ShareFormState = ShareFormState(),
    val drafts: List<Draft> = emptyList(),
    val connection: ConnectionUiState = ConnectionUiState(),
    val lastPostMessage: String? = null
)

class MainViewModel(
    private val repository: PosterRepository,
    private val titleFetcher: TitleFetcher = OkHttpTitleFetcher()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            loadSettings()
            refreshDrafts()
        }
    }

    private suspend fun loadSettings() {
        val settings = repository.loadSettings()
        _uiState.update { it.copy(settings = settings) }
    }

    private suspend fun refreshDrafts() {
        val drafts = repository.listDrafts()
        _uiState.update { it.copy(drafts = drafts) }
    }

    fun applySharedUrl(sharedUrl: String?) {
        if (sharedUrl.isNullOrBlank()) return
        _uiState.update { state ->
            state.copy(
                shareForm = state.shareForm.copy(
                    url = sharedUrl.trim(),
                    status = ShareStatus.Idle,
                    errorMessage = null
                )
            )
        }
        prefillTitle()
    }

    fun updateSettings(transform: (ShaarliSettings) -> ShaarliSettings) {
        _uiState.update { state -> state.copy(settings = transform(state.settings)) }
    }

    fun persistSettings() {
        viewModelScope.launch {
            repository.saveSettings(_uiState.value.settings)
        }
    }

    fun clearSettings() {
        viewModelScope.launch {
            repository.clearSettings()
            _uiState.value = AppUiState()
        }
    }

    fun updateShareForm(transform: (ShareFormState) -> ShareFormState) {
        _uiState.update { state -> state.copy(shareForm = transform(state.shareForm)) }
    }

    fun prefillTitle() {
        val url = uiState.value.shareForm.url
        if (url.isBlank()) return

        updateShareForm { form ->
            form.copy(status = ShareStatus.Prefilling, errorMessage = null, infoMessage = null)
        }

        viewModelScope.launch {
            val result = titleFetcher.fetchTitle(url)
            _uiState.update { state ->
                val updatedForm = if (result.isSuccess) {
                    state.shareForm.copy(
                        title = result.getOrThrow(),
                        status = ShareStatus.Idle,
                        errorMessage = null,
                        infoMessage = "Title prefetched"
                    )
                } else {
                    state.shareForm.copy(
                        status = ShareStatus.Error,
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to fetch title",
                        infoMessage = null
                    )
                }
                state.copy(shareForm = updatedForm)
            }
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(connection = ConnectionUiState(ConnectionStatus.Checking, "Checking…")) }
            val result = repository.testConnection(_uiState.value.settings)
            _uiState.update { state ->
                if (result.isSuccess) {
                    state.copy(
                        connection = ConnectionUiState(
                            status = ConnectionStatus.Online,
                            message = "Connection OK",
                            lastSuccessEpochMillis = System.currentTimeMillis()
                        )
                    )
                } else {
                    state.copy(
                        connection = ConnectionUiState(
                            status = ConnectionStatus.Error,
                            message = result.exceptionOrNull()?.message ?: "Connection failed"
                        )
                    )
                }
            }
        }
    }

    fun saveDraft() {
        val payload = buildPayloadOrFail() ?: return
        viewModelScope.launch {
            repository.saveDraft(payload)
            refreshDrafts()
            _uiState.update { state ->
                state.copy(
                    shareForm = state.shareForm.copy(
                        status = ShareStatus.Idle,
                        errorMessage = null,
                        infoMessage = "Draft saved"
                    )
                )
            }
        }
    }

    fun retryDrafts() {
        viewModelScope.launch {
            val result = repository.retryDrafts(_uiState.value.settings)
            refreshDrafts()
            _uiState.update { state ->
                state.copy(
                    lastPostMessage = "Retried drafts: posted ${result.posted}, remaining ${result.remaining}"
                )
            }
        }
    }

    fun postLink() {
        val payload = buildPayloadOrFail() ?: return
        updateShareForm { it.copy(status = ShareStatus.Posting, errorMessage = null, infoMessage = null) }
        viewModelScope.launch {
            val result = repository.postLink(_uiState.value.settings, payload)
            refreshDrafts()
            _uiState.update { state ->
                val shareForm = when (result.status) {
                    PostStatus.Posted -> ShareFormState(
                        status = ShareStatus.Success,
                        infoMessage = "Posted successfully"
                    )
                    PostStatus.Queued -> ShareFormState(
                        status = ShareStatus.Success,
                        infoMessage = result.message ?: "Queued for later"
                    )
                    PostStatus.Failed -> state.shareForm.copy(
                        status = ShareStatus.Error,
                        errorMessage = result.message
                    )
                }
                state.copy(
                    shareForm = shareForm,
                    lastPostMessage = result.message ?: state.lastPostMessage
                )
            }
        }
    }

    private fun buildPayloadOrFail(): LinkPayload? {
        val form = _uiState.value.shareForm
        if (form.url.isBlank()) {
            updateShareForm { it.copy(status = ShareStatus.Error, errorMessage = "URL is required") }
            return null
        }
        if (_uiState.value.settings.baseUrl.isBlank()) {
            updateShareForm { it.copy(status = ShareStatus.Error, errorMessage = "Shaarli URL not set") }
            return null
        }
        return LinkPayload.fromState(form)
    }

    companion object {
        fun provideFactory(repository: PosterRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return MainViewModel(repository) as T
                }
            }
        }
    }
}
