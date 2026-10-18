package com.livingrelation.markdownnotes.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.livingrelation.markdownnotes.core.markdown.Block
import com.livingrelation.markdownnotes.core.markdown.Document

@Composable
fun EditorScreen(
    document: Document,
    onDocumentChange: (Document) -> Unit,
    modifier: Modifier = Modifier,
) {
    var previewMode by rememberSaveable { mutableStateOf(false) }
    var selectedBlockIndex by rememberSaveable { mutableIntStateOf(-1) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .testTag("editor-screen"),
    ) {
        EditorModeToggle(
            previewMode = previewMode,
            onPreviewModeChange = { previewMode = it },
        )
        if (!previewMode) {
            EditorToolbar(
                selectedBlockIndex = selectedBlockIndex,
                onInsert = { type ->
                    val updated = document.insertBlockAfter(selectedBlockIndex.takeIf { it >= 0 }, type)
                    onDocumentChange(updated)
                    if (updated.blocks.isNotEmpty()) {
                        selectedBlockIndex =
                            when {
                                selectedBlockIndex < 0 -> 0
                                selectedBlockIndex >= updated.blocks.lastIndex -> updated.blocks.lastIndex
                                else -> selectedBlockIndex + 1
                            }
                    }
                },
            )
        }
        if (previewMode) {
            EditorPreview(document = document)
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(document.blocks, key = { index, _ -> index }) { index, block ->
                    BlockEditorCard(
                        index = index,
                        block = block,
                        selected = index == selectedBlockIndex,
                        onSelect = { selectedBlockIndex = index },
                        onBlockChange = { updatedBlock ->
                            onDocumentChange(document.updateBlock(index, updatedBlock))
                        },
                        onRemove = {
                            onDocumentChange(document.removeBlock(index))
                            if (selectedBlockIndex == index) {
                                selectedBlockIndex = -1
                            } else if (selectedBlockIndex > index) {
                                selectedBlockIndex -= 1
                            }
                        },
                        onMoveUp = {
                            onDocumentChange(document.moveBlockUp(index))
                            if (selectedBlockIndex == index) selectedBlockIndex = index - 1
                        },
                        onMoveDown = {
                            onDocumentChange(document.moveBlockDown(index))
                            if (selectedBlockIndex == index) selectedBlockIndex = index + 1
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorModeToggle(
    previewMode: Boolean,
    onPreviewModeChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            modifier = Modifier.testTag("editor-mode-edit"),
            selected = !previewMode,
            onClick = { onPreviewModeChange(false) },
            label = { Text("Edit") },
        )
        FilterChip(
            modifier = Modifier.testTag("editor-mode-preview"),
            selected = previewMode,
            onClick = { onPreviewModeChange(true) },
            label = { Text("Preview") },
        )
    }
}

@Composable
private fun EditorToolbar(
    selectedBlockIndex: Int,
    onInsert: (InsertableBlockType) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AssistChip(
            onClick = { onInsert(InsertableBlockType.Paragraph) },
            label = { Text("Paragraph") },
            modifier = Modifier.testTag("toolbar-add-paragraph"),
        )
        AssistChip(
            onClick = { onInsert(InsertableBlockType.Heading) },
            label = { Text("Heading") },
            modifier = Modifier.testTag("toolbar-add-heading"),
        )
        AssistChip(
            onClick = { onInsert(InsertableBlockType.CodeFence) },
            label = { Text("Code") },
            modifier = Modifier.testTag("toolbar-add-code"),
        )
        AssistChip(
            onClick = { onInsert(InsertableBlockType.HorizontalRule) },
            label = { Text("Rule") },
            modifier = Modifier.testTag("toolbar-add-rule"),
        )
        if (selectedBlockIndex >= 0) {
            Text(
                text = "Insert after block ${selectedBlockIndex + 1}",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 8.dp, top = 10.dp),
            )
        }
    }
}

@Composable
private fun BlockEditorCard(
    index: Int,
    block: Block,
    selected: Boolean,
    onSelect: () -> Unit,
    onBlockChange: (Block) -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    val borderModifier =
        if (selected) {
            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
        } else {
            Modifier
        }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("block-card-$index")
                .then(borderModifier)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = blockLabel(block, index),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                AssistChip(onClick = onSelect, label = { Text("Select") })
                AssistChip(onClick = onMoveUp, label = { Text("Up") })
                AssistChip(onClick = onMoveDown, label = { Text("Down") })
                AssistChip(onClick = onRemove, label = { Text("Delete") })
            }
        }
        when (block) {
            is Block.Heading -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 3).forEach { level ->
                        FilterChip(
                            selected = block.level == level,
                            onClick = { onBlockChange(block.copy(level = level)) },
                            label = { Text("H$level") },
                        )
                    }
                }
                OutlinedTextField(
                    value = block.text,
                    onValueChange = { onBlockChange(block.copy(text = it)) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag("block-heading-$index"),
                    label = { Text("Heading text") },
                    singleLine = false,
                )
            }
            is Block.Paragraph ->
                OutlinedTextField(
                    value = block.text,
                    onValueChange = { onBlockChange(block.copy(text = it)) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag("block-paragraph-$index"),
                    label = { Text("Paragraph") },
                    minLines = 2,
                )
            is Block.CodeFence -> {
                OutlinedTextField(
                    value = block.language.orEmpty(),
                    onValueChange = { lang ->
                        onBlockChange(block.copy(language = lang.takeIf { it.isNotBlank() }))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Language (optional)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = block.code,
                    onValueChange = { onBlockChange(block.copy(code = it)) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag("block-code-$index"),
                    label = { Text("Code") },
                    textStyle =
                        MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                    minLines = 4,
                )
            }
            Block.HorizontalRule -> {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Horizontal rule (no body text)",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            is Block.Table ->
                Text(
                    text = "Table (${block.rows.size} rows) — edit on the Table tab.",
                    style = MaterialTheme.typography.bodyMedium,
                )
        }
    }
}

private fun blockLabel(block: Block, index: Int): String {
    val type =
        when (block) {
            is Block.Heading -> "Heading H${block.level}"
            is Block.Paragraph -> "Paragraph"
            is Block.CodeFence -> "Code fence"
            Block.HorizontalRule -> "Horizontal rule"
            is Block.Table -> "Table"
        }
    return "Block ${index + 1}: $type"
}

@Composable
private fun EditorPreview(document: Document, modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .testTag("editor-preview")
                .padding(20.dp),
    ) {
        for (block in document.blocks) {
            PreviewBlockView(block = block)
        }
    }
}

@Composable
private fun PreviewBlockView(block: Block, modifier: Modifier = Modifier) {
    when (block) {
        is Block.Heading ->
            Text(
                text = block.text,
                style =
                    when (block.level) {
                        1 -> MaterialTheme.typography.headlineLarge
                        2 -> MaterialTheme.typography.headlineMedium
                        else -> MaterialTheme.typography.headlineSmall
                    },
                modifier = modifier.padding(vertical = 8.dp),
            )
        is Block.Paragraph ->
            Text(
                text = block.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = modifier.padding(vertical = 4.dp),
            )
        is Block.CodeFence ->
            Text(
                text = block.code,
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                modifier =
                    modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp),
            )
        Block.HorizontalRule ->
            HorizontalDivider(modifier = modifier.padding(vertical = 12.dp))
        is Block.Table -> {
            Column(modifier = modifier.padding(vertical = 8.dp)) {
                block.rows.forEachIndexed { rowIndex, row ->
                    val isHeader = block.headerRow && rowIndex == 0
                    Text(
                        text = row.joinToString("  ·  "),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
        }
    }
}
