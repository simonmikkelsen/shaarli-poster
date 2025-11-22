package com.shaarli.poster.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.shaarli.poster.data.model.AuthType
import com.shaarli.poster.data.model.ShaarliSettings
import com.shaarli.poster.util.UrlNormalizer

@Composable
fun SettingsSection(
    settings: ShaarliSettings,
    onBaseUrlChange: (String) -> Unit,
    onAuthTypeChange: (AuthType) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onApiSecretChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Instance settings")
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = settings.baseUrl,
                onValueChange = { value -> onBaseUrlChange(UrlNormalizer.normalizeBaseUrl(value)) },
                label = { Text("Shaarli URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Authentication")
            Row(modifier = Modifier.padding(top = 4.dp)) {
                FilterChip(
                    selected = settings.authType == AuthType.Token,
                    onClick = { onAuthTypeChange(AuthType.Token) },
                    label = { Text("API secret") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = settings.authType == AuthType.Session,
                    onClick = { onAuthTypeChange(AuthType.Session) },
                    label = { Text("Username/password") }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (settings.authType == AuthType.Token) {
                OutlinedTextField(
                    value = settings.apiSecret,
                    onValueChange = onApiSecretChange,
                    label = { Text("API secret/token") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            } else {
                OutlinedTextField(
                    value = settings.username,
                    onValueChange = onUsernameChange,
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = settings.password,
                    onValueChange = onPasswordChange,
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        }
    }
}
