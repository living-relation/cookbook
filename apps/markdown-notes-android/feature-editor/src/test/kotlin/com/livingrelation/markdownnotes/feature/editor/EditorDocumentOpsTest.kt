package com.livingrelation.markdownnotes.feature.editor

import com.livingrelation.markdownnotes.core.markdown.Block
import com.livingrelation.markdownnotes.core.markdown.Document
import org.junit.Assert.assertEquals
import org.junit.Test

class EditorDocumentOpsTest {
    private val sample =
        Document(
            blocks =
                listOf(
                    Block.Heading(1, "Title"),
                    Block.Paragraph("Body"),
                ),
        )

    @Test
    fun updateBlock_replacesAtIndex() {
        val updated = sample.updateBlock(1, Block.Paragraph("Changed"))
        assertEquals("Changed", (updated.blocks[1] as Block.Paragraph).text)
    }

    @Test
    fun insertBlockAfter_appendsWhenNoSelection() {
        val updated = sample.insertBlockAfter(null, InsertableBlockType.HorizontalRule)
        assertEquals(3, updated.blocks.size)
        assertEquals(Block.HorizontalRule, updated.blocks[2])
    }

    @Test
    fun moveBlockUp_swapsWithPrevious() {
        val updated = sample.moveBlockUp(1)
        assertEquals(Block.Paragraph("Body"), updated.blocks[0])
        assertEquals(Block.Heading(1, "Title"), updated.blocks[1])
    }
}
