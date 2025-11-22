package com.shaarli.poster.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shaarli.poster.data.model.ShareFormState
import com.shaarli.poster.data.model.ShareStatus

@Composable
fun ShareSection(
    shareForm: ShareFormState,
    onUrlChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onTagsChange: (String) -> Unit,
    onPrivateChange: (Boolean) -> Unit,
    onFetchTitle: () -> Unit,
    onPost: () -> Unit,
    onSaveDraft: () -> Unit,
    onRetryDrafts: () -> Unit,
    pendingDrafts: Int,
    lastPostMessage: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Share link")
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = shareForm.url,
                onValueChange = onUrlChange,
                label = { Text("URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = shareForm.title,
                onValueChange = onTitleChange,
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onFetchTitle,
                    enabled = shareForm.status != ShareStatus.Prefilling && shareForm.url.isNotBlank()
                ) {
                    Text(text = if (shareForm.status == ShareStatus.Prefilling) "Loading..." else "Fetch title")
                }
                TextButton(onClick = { onTitleChange("") }) {
                    Text("Clear title")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = shareForm.description,
                onValueChange = onDescriptionChange,
                label = { Text("Description / notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = shareForm.tags,
                onValueChange = onTagsChange,
                label = { Text("Tags (comma separated)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            TagPreview(tags = shareForm.tags)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    Switch(
                        checked = shareForm.isPrivate,
                        onCheckedChange = onPrivateChange
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (shareForm.isPrivate) "Private" else "Public")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onPost,
                    enabled = shareForm.url.isNotBlank() && shareForm.status != ShareStatus.Posting
                ) {
                    Text(if (shareForm.status == ShareStatus.Posting) "Posting..." else "Post")
                }
                Button(onClick = onSaveDraft, enabled = shareForm.url.isNotBlank()) {
                    Text("Save draft")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Pending drafts: $pendingDrafts")
                TextButton(onClick = onRetryDrafts, enabled = pendingDrafts > 0) {
                    Text("Retry drafts")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            StatusMessage(shareForm = shareForm, lastPostMessage = lastPostMessage)
        }
    }
}

@Composable
private fun StatusMessage(shareForm: ShareFormState, lastPostMessage: String?) {
    when (shareForm.status) {
        ShareStatus.Success -> Text(text = "Link posted successfully.")
        ShareStatus.Error -> if (!shareForm.errorMessage.isNullOrBlank()) {
            Text(text = shareForm.errorMessage)
        }
        ShareStatus.Prefilling -> Text(text = "Fetching title...")
        ShareStatus.Posting -> Text(text = "Posting...")
        ShareStatus.Idle -> {}
    }
    shareForm.infoMessage?.let { Text(it) }
    lastPostMessage?.let { Text("Last: $it") }
}

@Composable
private fun TagPreview(tags: String) {
    val parsed = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    if (parsed.isEmpty()) return
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(parsed.size) { index ->
            AssistChip(onClick = {}, label = { Text(parsed[index]) })
        }
    }
}
