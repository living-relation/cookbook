package com.livingrelation.markdownnotes.feature.table

import com.livingrelation.markdownnotes.core.markdown.Block
import com.livingrelation.markdownnotes.core.markdown.Document

fun Document.tableBlockIndices(): List<Int> =
    blocks.mapIndexedNotNull { index, block ->
        if (block is Block.Table) index else null
    }

fun Document.updateTableBlock(
    blockIndex: Int,
    table: Block.Table,
): Document {
    if (blockIndex !in blocks.indices) return this
    val existing = blocks[blockIndex]
    if (existing !is Block.Table) return this
    return copy(blocks = blocks.toMutableList().also { it[blockIndex] = table.normalized() })
}

fun Document.insertTableBlock(afterBlockIndex: Int? = null): Document {
    val newTable =
        Block.Table(
            headerRow = true,
            rows =
                listOf(
                    listOf("Column 1", "Column 2"),
                    listOf("", ""),
                ),
        ).normalized()
    val insertAt =
        when {
            blocks.isEmpty() -> 0
            afterBlockIndex == null -> blocks.size
            afterBlockIndex < 0 -> 0
            afterBlockIndex >= blocks.lastIndex -> blocks.size
            else -> afterBlockIndex + 1
        }
    val updated = blocks.toMutableList()
    updated.add(insertAt, newTable)
    return copy(blocks = updated)
}

fun Block.Table.normalized(): Block.Table {
    if (rows.isEmpty()) {
        return copy(rows = listOf(listOf("")))
    }
    val columnCount = rows.maxOf { it.size }.coerceAtLeast(1)
    val padded =
        rows.map { row ->
            if (row.size >= columnCount) row else row + List(columnCount - row.size) { "" }
        }
    return copy(rows = padded)
}

fun Block.Table.columnCount(): Int = rows.maxOfOrNull { it.size } ?: 1

fun Block.Table.updateCell(
    rowIndex: Int,
    columnIndex: Int,
    value: String,
): Block.Table {
    val table = normalized()
    if (rowIndex !in table.rows.indices) return table
    val row = table.rows[rowIndex].toMutableList()
    if (columnIndex !in row.indices) return table
    row[columnIndex] = value
    return table.copy(rows = table.rows.toMutableList().also { it[rowIndex] = row })
}

fun Block.Table.insertRow(at: Int): Block.Table {
    val table = normalized()
    val columns = table.columnCount()
    val emptyRow = List(columns) { "" }
    val insertAt = at.coerceIn(0, table.rows.size)
    val updated = table.rows.toMutableList()
    updated.add(insertAt, emptyRow)
    return table.copy(rows = updated)
}

fun Block.Table.deleteRow(at: Int): Block.Table {
    val table = normalized()
    if (table.rows.size <= 1 || at !in table.rows.indices) return table
    return table.copy(rows = table.rows.filterIndexed { index, _ -> index != at })
}

fun Block.Table.insertColumn(at: Int): Block.Table {
    val table = normalized()
    val insertAt = at.coerceIn(0, table.columnCount())
    val updated =
        table.rows.map { row ->
            val cells = row.toMutableList()
            cells.add(insertAt, "")
            cells
        }
    return table.copy(rows = updated)
}

fun Block.Table.deleteColumn(at: Int): Block.Table {
    val table = normalized()
    val columns = table.columnCount()
    if (columns <= 1 || at !in 0 until columns) return table
    val updated =
        table.rows.map { row ->
            row.filterIndexed { index, _ -> index != at }
        }
    return table.copy(rows = updated)
}
