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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    SettingsSection(
                        settings = uiState.value.settings,
                        connection = uiState.value.connection,
                        onBaseUrlChange = { value ->
                            mainViewModel.updateSettings { current ->
                                current.copy(baseUrl = value)
                            }
                        },
                        onAuthTypeChange = { authType ->
                            mainViewModel.updateSettings { current ->
                                current.copy(authType = authType)
                            }
                        },
                        onUsernameChange = { username ->
                            mainViewModel.updateSettings { current ->
                                current.copy(username = username)
                            }
                        },
                        onPasswordChange = { password ->
                            mainViewModel.updateSettings { current ->
                                current.copy(password = password)
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
                item {
                    ShareSection(
                        shareForm = uiState.value.shareForm,
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
                        pendingDrafts = uiState.value.drafts.size,
                        lastPostMessage = uiState.value.lastPostMessage
                    )
                }
                if (uiState.value.drafts.isNotEmpty()) {
                    item {
                        DraftListSection(
                            drafts = uiState.value.drafts,
                            onRetryDrafts = { mainViewModel.retryDrafts() }
                        )
                    }
                }
            }
        }
    }
}
