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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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
    private val _postSuccessEvents = Channel<Unit>(Channel.BUFFERED)
    val postSuccessEvents = _postSuccessEvents.receiveAsFlow()
    private var lastLookupUrl: String? = null
    private val logTag = "MainViewModel"
    private var settingsLoaded: Boolean = false

    init {
        viewModelScope.launch {
            loadSettings()
            refreshDrafts()
        }
    }

    private suspend fun loadSettings() {
        val settings = repository.loadSettings()
        settingsLoaded = true
        println("[$logTag] loadSettings baseUrl='${settings.baseUrl}' apiSecretBlank=${settings.apiSecret.isBlank()}")
        _uiState.update { it.copy(settings = settings) }

        val currentUrl = _uiState.value.shareForm.url
        if (currentUrl.isNotBlank() && _uiState.value.shareForm.existingLinkId == null) {
            println("[$logTag] loadSettings triggering lookup for existing url='$currentUrl'")
            fetchExistingLinkOrPrefill(currentUrl)
        }
    }

    private suspend fun refreshDrafts() {
        val drafts = repository.listDrafts()
        _uiState.update { it.copy(drafts = drafts) }
    }

    fun applySharedUrl(sharedUrl: String?) {
        if (sharedUrl.isNullOrBlank()) {
            println("[$logTag] applySharedUrl called with null/blank; ignoring")
            return
        }
        val trimmedUrl = sharedUrl.trim()
        println("[$logTag] applySharedUrl url='$trimmedUrl'")
        _uiState.update { state ->
            state.copy(
                shareForm = state.shareForm.copy(
                    url = trimmedUrl,
                    status = ShareStatus.Idle,
                    errorMessage = null,
                    infoMessage = null,
                    existingLinkId = null
                )
            )
        }
        fetchExistingLinkOrPrefill(trimmedUrl)
    }

    fun onUrlChanged(url: String, triggerLookup: Boolean) {
        val trimmedUrl = url.trim()
        println("[$logTag] onUrlChanged trimmed='$trimmedUrl' triggerLookup=$triggerLookup lastLookupUrl=$lastLookupUrl")
        updateShareForm { current ->
            current.copy(
                url = trimmedUrl,
                existingLinkId = null,
                infoMessage = null,
                errorMessage = null
            )
        }
        if (triggerLookup && trimmedUrl.isNotBlank() && trimmedUrl != lastLookupUrl) {
            println("[$logTag] onUrlChanged triggering lookup for '$trimmedUrl'")
            fetchExistingLinkOrPrefill(trimmedUrl)
        }
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

        println("[$logTag] prefillTitle for url='$url'")
        updateShareForm { form ->
            form.copy(status = ShareStatus.Prefilling, errorMessage = null, infoMessage = null)
        }

        viewModelScope.launch {
            val result = titleFetcher.fetchTitle(url)
            println("[$logTag] prefillTitle result isSuccess=${result.isSuccess} message='${result.exceptionOrNull()?.message}'")
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
        println("[$logTag] postLink payload.url='${payload.url}' id=${payload.id} existingLinkId=${_uiState.value.shareForm.existingLinkId}")
        updateShareForm { it.copy(status = ShareStatus.Posting, errorMessage = null, infoMessage = "Posting…") }
        viewModelScope.launch {
            val targetPayload = if (uiState.value.shareForm.existingLinkId != null) {
                payload.copy(id = uiState.value.shareForm.existingLinkId)
            } else {
                payload
            }
            val result = if (targetPayload.id != null) {
                repository.updateLink(_uiState.value.settings, targetPayload)
            } else {
                repository.postLink(_uiState.value.settings, targetPayload)
            }
            refreshDrafts()
            _uiState.update { state ->
                val shareForm = when (result.status) {
                    PostStatus.Posted -> ShareFormState(
                        status = ShareStatus.Success,
                        infoMessage = result.message ?: "Posted successfully"
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
            if (result.status == PostStatus.Posted || result.status == PostStatus.Queued) {
                _postSuccessEvents.trySend(Unit)
            }
        }
    }

    private fun buildPayloadOrFail(): LinkPayload? {
        val form = _uiState.value.shareForm
        if (form.url.isBlank()) {
            println("[$logTag][WARN] buildPayloadOrFail missing URL")
            updateShareForm { it.copy(status = ShareStatus.Error, errorMessage = "URL is required") }
            return null
        }
        if (_uiState.value.settings.baseUrl.isBlank()) {
            println("[$logTag][WARN] buildPayloadOrFail missing Shaarli base URL")
            updateShareForm { it.copy(status = ShareStatus.Error, errorMessage = "Shaarli URL not set") }
            return null
        }
        return LinkPayload.fromState(form)
    }

    private fun fetchExistingLinkOrPrefill(url: String) {
        val trimmedUrl = url.trim()
        if (trimmedUrl.isBlank()) return
        lastLookupUrl = trimmedUrl
        println("[$logTag] fetchExistingLinkOrPrefill start url='$trimmedUrl'")

        if (!settingsLoaded) {
            println("[$logTag] fetchExistingLinkOrPrefill settings not loaded yet; skipping lookup for now")
            return
        }

        val settings = _uiState.value.settings
        if (settings.baseUrl.isBlank() || settings.apiSecret.isBlank()) {
            println("[$logTag] fetchExistingLinkOrPrefill settings incomplete (baseUrl or apiSecret blank); using prefillTitle")
            prefillTitle()
            return
        }

        viewModelScope.launch {
            val result = repository.findExistingLink(_uiState.value.settings, trimmedUrl)
            val currentUrl = _uiState.value.shareForm.url.trim()
            println("[$logTag] fetchExistingLinkOrPrefill got result isSuccess=${result.isSuccess} currentUrl='$currentUrl'")
            if (currentUrl != trimmedUrl) return@launch
            result.fold(
                onSuccess = { existing ->
                    if (existing != null) {
                        println("[$logTag] fetchExistingLinkOrPrefill found existing id=${existing.id} url='${existing.url}' title='${existing.title}'")
                        _uiState.update { state ->
                            state.copy(
                                shareForm = state.shareForm.copy(
                                    title = existing.title,
                                    description = existing.description,
                                    tags = existing.tags.joinToString(", "),
                                    isPrivate = existing.isPrivate,
                                    existingLinkId = existing.id,
                                    infoMessage = "Existing link loaded",
                                    status = ShareStatus.Idle,
                                    errorMessage = null
                                )
                            )
                        }
                    } else {
                        println("[$logTag] fetchExistingLinkOrPrefill no existing found -> prefillTitle")
                        prefillTitle()
                    }
                },
                onFailure = {
                    println("[$logTag][WARN] fetchExistingLinkOrPrefill failed: ${it.message}")
                    prefillTitle()
                }
            )
        }
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
