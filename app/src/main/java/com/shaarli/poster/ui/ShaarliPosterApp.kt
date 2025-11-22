package com.shaarli.poster.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shaarli.poster.ui.screens.SettingsSection
import com.shaarli.poster.ui.screens.ShareSection
import com.shaarli.poster.ui.screens.DraftListSection
import com.shaarli.poster.ui.theme.ShaarliPosterTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShaarliPosterApp(
    sharedUrlState: State<String?>,
    isShareFlow: Boolean,
    mainViewModel: MainViewModel
) {
    val uiState = mainViewModel.uiState.collectAsState()

    LaunchedEffect(sharedUrlState.value) {
        mainViewModel.applySharedUrl(sharedUrlState.value)
    }

    ShaarliPosterTheme {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(text = "Shaarli Poster") },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
                )
            }
        ) { padding ->
            if (isShareFlow) {
                ShareScreen(
                    padding = padding,
                    uiStateValue = uiState.value,
                    mainViewModel = mainViewModel
                )
            } else {
                SettingsScreen(
                    padding = padding,
                    uiStateValue = uiState.value,
                    mainViewModel = mainViewModel
                )
            }
        }
    }
}

@Composable
private fun ShareScreen(
    padding: PaddingValues,
    uiStateValue: AppUiState,
    mainViewModel: MainViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            ShareSection(
                shareForm = uiStateValue.shareForm,
                onUrlChange = { url ->
                    mainViewModel.updateShareForm { current ->
                        current.copy(url = url)
                    }
                },
                onTitleChange = { title ->
                    mainViewModel.updateShareForm { current ->
                        current.copy(title = title)
                    }
                },
                onDescriptionChange = { desc ->
                    mainViewModel.updateShareForm { current ->
                        current.copy(description = desc)
                    }
                },
                onTagsChange = { tags ->
                    mainViewModel.updateShareForm { current ->
                        current.copy(tags = tags)
                    }
                },
                onPrivateChange = { isPrivate ->
                    mainViewModel.updateShareForm { current ->
                        current.copy(isPrivate = isPrivate)
                    }
                },
                onFetchTitle = { mainViewModel.prefillTitle() },
                onPost = { mainViewModel.postLink() },
                onSaveDraft = { mainViewModel.saveDraft() },
                onRetryDrafts = { mainViewModel.retryDrafts() },
                pendingDrafts = uiStateValue.drafts.size,
                lastPostMessage = uiStateValue.lastPostMessage
            )
        }
        if (uiStateValue.drafts.isNotEmpty()) {
            item {
                DraftListSection(
                    drafts = uiStateValue.drafts,
                    onRetryDrafts = { mainViewModel.retryDrafts() }
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    padding: PaddingValues,
    uiStateValue: AppUiState,
    mainViewModel: MainViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            SettingsSection(
                settings = uiStateValue.settings,
                connection = uiStateValue.connection,
                onBaseUrlChange = { value ->
                    mainViewModel.updateSettings { current ->
                        current.copy(baseUrl = value)
                    }
                },
                onApiSecretChange = { secret ->
                    mainViewModel.updateSettings { current ->
                        current.copy(apiSecret = secret)
                    }
                },
                onSaveSettings = { mainViewModel.persistSettings() },
                onTestConnection = { mainViewModel.testConnection() },
                onClearCredentials = { mainViewModel.clearSettings() }
            )
        }
    }
}
