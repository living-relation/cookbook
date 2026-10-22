package com.livingrelation.markdownnotes.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.livingrelation.markdownnotes.core.files.RecentNote

@Composable
fun RecentsSheet(
    recents: List<RecentNote>,
    onRecentSelected: (RecentNote) -> Unit,
    onOpenFile: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered =
        remember(recents, query) {
            val needle = query.trim()
            if (needle.isEmpty()) {
                recents
            } else {
                recents.filter { it.displayName.contains(needle, ignoreCase = true) }
            }
        }
    Column(modifier = Modifier.padding(bottom = 32.dp)) {
        Text(
            text = "Find a markdown file",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        Button(
            onClick = onOpenFile,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .testTag("recents-open-file"),
        ) {
            Text("Search files on this phone")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .testTag("recents-search"),
            label = { Text("Filter recents") },
            singleLine = true,
        )
        if (filtered.isEmpty()) {
            Text(
                text = "No matches yet. Use Search files on this phone.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
        } else {
            LazyColumn {
                items(filtered, key = { it.uriString }) { note ->
                    Text(
                        text = note.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { onRecentSelected(note) }
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}
