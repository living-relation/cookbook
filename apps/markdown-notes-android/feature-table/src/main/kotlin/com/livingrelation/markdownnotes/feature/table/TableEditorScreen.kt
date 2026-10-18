package com.livingrelation.markdownnotes.feature.table

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
            text = "No tables in this note",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Insert a GFM table block to edit cells here. Saving uses pipe escaping for cell text.",
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
        tableIndices.forEach { blockIndex ->
            FilterChip(
                selected = blockIndex == selectedBlockIndex,
                onClick = { onSelect(blockIndex) },
                label = { Text("Table block ${blockIndex + 1}") },
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
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        normalized.rows.forEachIndexed { rowIndex, row ->
            val isHeader = table.headerRow && rowIndex == 0
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                row.forEachIndexed { columnIndex, cell ->
                    OutlinedTextField(
                        value = cell,
                        onValueChange = { onCellChange(rowIndex, columnIndex, it) },
                        modifier =
                            Modifier
                                .widthIn(min = 120.dp, max = 200.dp)
                                .testTag("table-cell-$rowIndex-$columnIndex"),
                        label = {
                            Text(
                                if (isHeader) "H${columnIndex + 1}" else "R${rowIndex + 1}C${columnIndex + 1}",
                            )
                        },
                        textStyle =
                            MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal,
                            ),
                        singleLine = false,
                        maxLines = 4,
                    )
                }
            }
        }
        Text(
            text = "Row 1 is the header row when saved to Markdown.",
            style = MaterialTheme.typography.labelMedium,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(12.dp),
        )
    }
}
