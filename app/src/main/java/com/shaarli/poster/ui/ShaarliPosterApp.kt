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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shaarli.poster.data.model.AuthType
import com.shaarli.poster.ui.screens.SettingsSection
import com.shaarli.poster.ui.screens.ShareSection
import com.shaarli.poster.ui.theme.ShaarliPosterTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShaarliPosterApp(
    sharedUrlState: State<String?>,
    mainViewModel: MainViewModel = viewModel()
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
                        }
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
                        onPost = { mainViewModel.postLink() }
                    )
                }
            }
        }
    }
}
