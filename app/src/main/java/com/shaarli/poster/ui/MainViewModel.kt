package com.shaarli.poster.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shaarli.poster.data.metadata.OkHttpTitleFetcher
import com.shaarli.poster.data.metadata.TitleFetcher
import com.shaarli.poster.data.model.ShareFormState
import com.shaarli.poster.data.model.ShareStatus
import com.shaarli.poster.data.model.ShaarliSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppUiState(
    val settings: ShaarliSettings = ShaarliSettings(),
    val shareForm: ShareFormState = ShareFormState()
)

class MainViewModel(
    private val titleFetcher: TitleFetcher = OkHttpTitleFetcher()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

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

    fun updateShareForm(transform: (ShareFormState) -> ShareFormState) {
        _uiState.update { state -> state.copy(shareForm = transform(state.shareForm)) }
    }

    fun prefillTitle() {
        val url = uiState.value.shareForm.url
        if (url.isBlank()) return

        updateShareForm { form ->
            form.copy(status = ShareStatus.Prefilling, errorMessage = null)
        }

        viewModelScope.launch {
            val result = titleFetcher.fetchTitle(url)
            _uiState.update { state ->
                val updatedForm = if (result.isSuccess) {
                    state.shareForm.copy(
                        title = result.getOrThrow(),
                        status = ShareStatus.Idle,
                        errorMessage = null
                    )
                } else {
                    state.shareForm.copy(
                        status = ShareStatus.Error,
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to fetch title"
                    )
                }
                state.copy(shareForm = updatedForm)
            }
        }
    }

    fun postLink() {
        updateShareForm { form ->
            form.copy(status = ShareStatus.Posting, errorMessage = null)
        }

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    shareForm = state.shareForm.copy(
                        status = ShareStatus.Error,
                        errorMessage = "Posting not implemented yet"
                    )
                )
            }
        }
    }
}
