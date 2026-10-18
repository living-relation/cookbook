package com.livingrelation.markdownnotes.feature.reader

import com.livingrelation.markdownnotes.core.markdown.Block
import com.livingrelation.markdownnotes.core.markdown.Document
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderTocTest {
    @Test
    fun tableOfContents_listsHeadingsWithBlockIndex() {
        val document =
            Document(
                blocks =
                    listOf(
                        Block.Heading(1, "Intro"),
                        Block.Paragraph("Body"),
                        Block.Heading(2, "Details"),
                        Block.CodeFence(null, "code"),
                    ),
            )

        assertEquals(
            listOf(
                TocEntry(0, 1, "Intro"),
                TocEntry(2, 2, "Details"),
            ),
            document.tableOfContents(),
        )
    }
}
