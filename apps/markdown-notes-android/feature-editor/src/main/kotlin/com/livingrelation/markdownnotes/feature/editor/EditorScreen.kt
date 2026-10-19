package com.livingrelation.markdownnotes.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.livingrelation.markdownnotes.core.markdown.Block
import com.livingrelation.markdownnotes.core.markdown.Document
import com.livingrelation.markdownnotes.feature.reader.MarkdownTable

@Composable
fun EditorScreen(
    document: Document,
    onDocumentChange: (Document) -> Unit,
    modifier: Modifier = Modifier,
    onEditTable: () -> Unit = {},
) {
    var selectedBlockIndex by rememberSaveable { mutableIntStateOf(-1) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .testTag("editor-screen"),
    ) {
        EditorToolbar(
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
                    onEditTable = onEditTable,
                )
            }
        }
    }
}

@Composable
private fun EditorToolbar(onInsert: (InsertableBlockType) -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = false,
            onClick = { onInsert(InsertableBlockType.Paragraph) },
            label = { Text("Paragraph") },
            modifier = Modifier.testTag("toolbar-add-paragraph"),
        )
        FilterChip(
            selected = false,
            onClick = { onInsert(InsertableBlockType.Heading) },
            label = { Text("Heading") },
            modifier = Modifier.testTag("toolbar-add-heading"),
        )
        FilterChip(
            selected = false,
            onClick = { onInsert(InsertableBlockType.CodeFence) },
            label = { Text("Code") },
            modifier = Modifier.testTag("toolbar-add-code"),
        )
        FilterChip(
            selected = false,
            onClick = { onInsert(InsertableBlockType.Table) },
            label = { Text("Table") },
            modifier = Modifier.testTag("toolbar-add-table"),
        )
        FilterChip(
            selected = false,
            onClick = { onInsert(InsertableBlockType.HorizontalRule) },
            label = { Text("Line") },
            modifier = Modifier.testTag("toolbar-add-rule"),
        )
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
    onEditTable: () -> Unit,
) {
    val borderModifier =
        if (selected) {
            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
        } else {
            Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
        }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("block-card-$index")
                .then(borderModifier)
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onSelect)
                .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = blockKindLabel(block),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            IconButton(onClick = onMoveUp, modifier = Modifier.testTag("block-up-$index")) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move up")
            }
            IconButton(onClick = onMoveDown, modifier = Modifier.testTag("block-down-$index")) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move down")
            }
            IconButton(onClick = onRemove, modifier = Modifier.testTag("block-delete-$index")) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete")
            }
        }
        when (block) {
            is Block.Heading -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
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
                    placeholder = { Text("Heading") },
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
                    placeholder = { Text("Write here") },
                    minLines = 2,
                )
            is Block.CodeFence -> {
                OutlinedTextField(
                    value = block.language.orEmpty(),
                    onValueChange = { lang ->
                        onBlockChange(block.copy(language = lang.takeIf { it.isNotBlank() }))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Language") },
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
                    placeholder = { Text("Code") },
                    textStyle =
                        MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                    minLines = 4,
                )
            }
            Block.HorizontalRule -> {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            is Block.Table -> {
                MarkdownTable(
                    table = block,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
                TextButton(
                    onClick = onEditTable,
                    modifier = Modifier.testTag("block-edit-table-$index"),
                ) {
                    Text("Edit cells")
                }
            }
        }
    }
}

private fun blockKindLabel(block: Block): String =
    when (block) {
        is Block.Heading -> "Heading"
        is Block.Paragraph -> "Text"
        is Block.CodeFence -> "Code"
        Block.HorizontalRule -> "Line"
        is Block.Table -> "Table"
    }
