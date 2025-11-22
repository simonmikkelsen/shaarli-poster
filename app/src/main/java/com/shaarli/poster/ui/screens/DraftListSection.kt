package com.shaarli.poster.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shaarli.poster.data.model.Draft
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DraftListSection(
    drafts: List<Draft>,
    onRetryDrafts: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Pending drafts (${drafts.size})")
            Spacer(modifier = Modifier.height(8.dp))
            drafts.forEach { draft ->
                DraftRow(draft)
                Spacer(modifier = Modifier.height(8.dp))
            }
            Button(onClick = onRetryDrafts, enabled = drafts.isNotEmpty()) {
                Text("Retry all drafts")
            }
        }
    }
}

@Composable
private fun DraftRow(draft: Draft) {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val timestamp = formatter.format(Date(draft.createdAtEpochMillis))
    Text(text = draft.payload.title.ifBlank { draft.payload.url })
    Text(text = draft.payload.url)
    Text(text = "Tags: ${draft.payload.tags.joinToString(", ")} • $timestamp")
}
