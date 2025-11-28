package com.shaarli.poster.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalUriHandler
import com.shaarli.poster.data.model.ShaarliSettings
import com.shaarli.poster.ui.ConnectionUiState
import com.shaarli.poster.ui.ConnectionStatus
import com.shaarli.poster.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsSection(
    settings: ShaarliSettings,
    connection: ConnectionUiState,
    onBaseUrlChange: (String) -> Unit,
    onApiSecretChange: (String) -> Unit,
    onSaveSettings: () -> Unit,
    onTestConnection: () -> Unit,
    onClearCredentials: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val showLicense = remember { mutableStateOf(false) }
    val licenseText = produceState(initialValue = "") {
        val text = runCatching {
            context.resources.openRawResource(R.raw.license)
                .bufferedReader()
                .use { it.readText() }
        }.getOrDefault("")
        value = text
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Instance settings")
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = settings.baseUrl,
                onValueChange = { value -> onBaseUrlChange(value) },
                label = { Text("Shaarli URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "REST API secreet (from Tools, Configure your Shaarli)")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = settings.apiSecret,
                onValueChange = onApiSecretChange,
                label = { Text("API secret") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onSaveSettings, modifier = Modifier.fillMaxWidth()) {
                Text("Save settings")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onClearCredentials, modifier = Modifier.fillMaxWidth()) {
                Text("Clear credentials")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onTestConnection, modifier = Modifier.fillMaxWidth()) {
                Text("Test connection")
            }
            Spacer(modifier = Modifier.height(8.dp))
            ConnectionStatusText(connection)
            if (settings.baseUrl.startsWith("http://")) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Warning: HTTP is not secure; prefer HTTPS.")
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Tip: Find the REST API secret by going to your Shaarli, the menu Tools, Configure your Shaarli. Then check Enable REST API and copy the REST API secret from the field below it.")
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Free software by Simon Mikkelsen", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "https://github.com/simonmikkelsen/shaarli-poster/",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { uriHandler.openUri("https://github.com/simonmikkelsen/shaarli-poster/") }
            )
            Text(
                text = "License",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { showLicense.value = true }
            )
        }
    }
    if (showLicense.value) {
        AlertDialog(
            onDismissRequest = { showLicense.value = false },
            confirmButton = {
                TextButton(onClick = { showLicense.value = false }) {
                    Text("Close")
                }
            },
            title = { Text("License") },
            text = {
                val scrollState = rememberScrollState()
                Text(
                    text = licenseText.value.ifBlank { "License unavailable" },
                    modifier = Modifier
                        .height(300.dp)
                        .verticalScroll(scrollState)
                )
            }
        )
    }
}

@Composable
private fun ConnectionStatusText(connection: ConnectionUiState) {
    val label = when (connection.status) {
        ConnectionStatus.Idle -> "Status: idle"
        ConnectionStatus.Checking -> "Status: checking..."
        ConnectionStatus.Online -> "Status: online"
        ConnectionStatus.Offline -> "Status: offline"
        ConnectionStatus.Error -> "Status: error"
    }
    Text(text = listOfNotNull(label, connection.message).joinToString(" • "))
    connection.lastSuccessEpochMillis?.let { last ->
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        Text(text = "Last success: ${formatter.format(Date(last))}")
    }
}
