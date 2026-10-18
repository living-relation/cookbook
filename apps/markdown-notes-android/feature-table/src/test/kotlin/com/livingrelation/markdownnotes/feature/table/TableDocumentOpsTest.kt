package com.livingrelation.markdownnotes.feature.table

import com.livingrelation.markdownnotes.core.markdown.Block
import com.livingrelation.markdownnotes.core.markdown.Document
import com.livingrelation.markdownnotes.core.markdown.MarkdownEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TableDocumentOpsTest {
    private val engine = MarkdownEngine()

    private val sampleTable =
        Block.Table(
            headerRow = true,
            rows =
                listOf(
                    listOf("A", "B"),
                    listOf("1", "2"),
                ),
        )

    @Test
    fun tableBlockIndices_listsOnlyTableBlocks() {
        val doc =
            Document(
                blocks =
                    listOf(
                        Block.Paragraph("x"),
                        sampleTable,
                        Block.Heading(1, "h"),
                        sampleTable.copy(rows = listOf(listOf("only"))),
                    ),
            )
        assertEquals(listOf(1, 3), doc.tableBlockIndices())
    }

    @Test
    fun normalized_padsRaggedRows() {
        val table =
            Block.Table(
                headerRow = true,
                rows =
                    listOf(
                        listOf("a", "b", "c"),
                        listOf("1"),
                    ),
            )
        assertEquals(listOf("a", "b", "c"), table.normalized().rows[0])
        assertEquals(listOf("1", "", ""), table.normalized().rows[1])
    }

    @Test
    fun insertRowAndColumn_updatesGrid() {
        var table = sampleTable.normalized()
        table = table.insertRow(1)
        assertEquals(3, table.rows.size)
        assertEquals(listOf("", ""), table.rows[1])

        table = table.insertColumn(1)
        assertEquals(3, table.columnCount())
        assertEquals("", table.rows[0][1])
    }

    @Test
    fun deleteRowAndColumn_keepsMinimumShape() {
        var table = sampleTable.normalized()
        table = table.deleteRow(5)
        assertEquals(2, table.rows.size)

        table = table.deleteRow(0)
        assertEquals(1, table.rows.size)

        table = table.deleteColumn(0)
        assertEquals(1, table.columnCount())
        table = table.deleteColumn(0)
        assertEquals(1, table.columnCount())
    }

    @Test
    fun updateCell_roundTripsThroughMarkdownEngine() {
        val doc = Document(blocks = listOf(sampleTable))
        val updated =
            doc.updateTableBlock(
                0,
                sampleTable.updateCell(1, 1, "pipe | value"),
            )
        val table = updated.blocks[0] as Block.Table
        assertEquals("pipe | value", table.rows[1][1])

        val markdown = engine.emit(updated)
        assertTrue(markdown.contains("pipe \\| value"))
        val reparsed = engine.parse(markdown)
        assertEquals(updated, reparsed)
    }

    @Test
    fun insertTableBlock_appendsDefaultTable() {
        val doc = Document(blocks = listOf(Block.Paragraph("p")))
        val updated = doc.insertTableBlock()
        assertEquals(2, updated.blocks.size)
        val table = updated.blocks[1] as Block.Table
        assertEquals(2, table.rows.size)
        assertEquals(2, table.columnCount())
    }
}
