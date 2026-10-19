package com.livingrelation.markdownnotes.feature.table

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.livingrelation.markdownnotes.core.markdown.Block
import com.livingrelation.markdownnotes.core.markdown.Document

@Composable
fun TableEditorScreen(
    document: Document,
    onDocumentChange: (Document) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tableIndices = document.tableBlockIndices()
    var selectedTableIndex by rememberSaveable(tableIndices.size) {
        mutableIntStateOf(tableIndices.firstOrNull() ?: -1)
    }
    LaunchedEffect(tableIndices, selectedTableIndex) {
        if (selectedTableIndex !in tableIndices) {
            selectedTableIndex = tableIndices.firstOrNull() ?: -1
        }
    }
    val blockIndex =
        if (selectedTableIndex in tableIndices) {
            selectedTableIndex
        } else {
            tableIndices.firstOrNull() ?: -1
        }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .testTag("table-screen"),
    ) {
        if (tableIndices.isEmpty()) {
            TableEmptyState(onInsertTable = { onDocumentChange(document.insertTableBlock()) })
            return
        }

        TableSelectorRow(
            tableIndices = tableIndices,
            selectedBlockIndex = blockIndex,
            onSelect = { selectedTableIndex = it },
        )

        val table = document.blocks[blockIndex] as Block.Table
        TableToolbar(
            onAddRow = {
                onDocumentChange(
                    document.updateTableBlock(blockIndex, table.insertRow(table.rows.size)),
                )
            },
            onDeleteRow = {
                onDocumentChange(
                    document.updateTableBlock(blockIndex, table.deleteRow(table.rows.lastIndex)),
                )
            },
            onAddColumn = {
                onDocumentChange(
                    document.updateTableBlock(blockIndex, table.insertColumn(table.columnCount())),
                )
            },
            onDeleteColumn = {
                onDocumentChange(
                    document.updateTableBlock(
                        blockIndex,
                        table.deleteColumn(table.columnCount() - 1),
                    ),
                )
            },
        )
        TableGrid(
            table = table,
            onCellChange = { row, column, value ->
                onDocumentChange(
                    document.updateTableBlock(blockIndex, table.updateCell(row, column, value)),
                )
            },
        )
    }
}

@Composable
private fun TableEmptyState(onInsertTable: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "No tables in this file",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Insert a table, then tap a cell to type.",
            style = MaterialTheme.typography.bodyMedium,
        )
        AssistChip(
            onClick = onInsertTable,
            label = { Text("Insert table") },
            modifier = Modifier.testTag("table-insert-empty"),
        )
    }
}

@Composable
private fun TableSelectorRow(
    tableIndices: List<Int>,
    selectedBlockIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tableIndices.forEachIndexed { ordinal, blockIndex ->
            FilterChip(
                selected = blockIndex == selectedBlockIndex,
                onClick = { onSelect(blockIndex) },
                label = { Text("Table ${ordinal + 1}") },
                modifier = Modifier.testTag("table-select-$blockIndex"),
            )
        }
    }
}

@Composable
private fun TableToolbar(
    onAddRow: () -> Unit,
    onDeleteRow: () -> Unit,
    onAddColumn: () -> Unit,
    onDeleteColumn: () -> Unit,
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
            onClick = onAddRow,
            label = { Text("Add row") },
            modifier = Modifier.testTag("table-add-row"),
        )
        AssistChip(
            onClick = onDeleteRow,
            label = { Text("Delete row") },
            modifier = Modifier.testTag("table-delete-row"),
        )
        AssistChip(
            onClick = onAddColumn,
            label = { Text("Add column") },
            modifier = Modifier.testTag("table-add-column"),
        )
        AssistChip(
            onClick = onDeleteColumn,
            label = { Text("Delete column") },
            modifier = Modifier.testTag("table-delete-column"),
        )
    }
}

@Composable
private fun TableGrid(
    table: Block.Table,
    onCellChange: (row: Int, column: Int, value: String) -> Unit,
) {
    val normalized = table.normalized()
    val columnCount = normalized.columnCount()
    val shape = RoundedCornerShape(10.dp)
    val border = MaterialTheme.colorScheme.outline
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .clip(shape)
                    .border(1.dp, border, shape),
        ) {
            normalized.rows.forEachIndexed { rowIndex, row ->
                val isHeader = table.headerRow && rowIndex == 0
                Row {
                    repeat(columnCount) { columnIndex ->
                        val cell = row.getOrElse(columnIndex) { "" }
                        BasicTextField(
                            value = cell,
                            onValueChange = { onCellChange(rowIndex, columnIndex, it) },
                            textStyle =
                                MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface,
                                ),
                            modifier =
                                Modifier
                                    .width(148.dp)
                                    .heightIn(min = 48.dp)
                                    .background(
                                        if (isHeader) {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                    )
                                    .border(0.5.dp, border.copy(alpha = 0.7f))
                                    .padding(horizontal = 10.dp, vertical = 12.dp)
                                    .testTag("table-cell-$rowIndex-$columnIndex"),
                        )
                    }
                }
            }
        }
        Text(
            text = "First row is the header when saved.",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
