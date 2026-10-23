package com.livingrelation.markdownnotes.feature.reader

import com.livingrelation.markdownnotes.core.markdown.MarkdownOutline
import com.livingrelation.markdownnotes.core.markdown.TocEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderTocTest {
    @Test
    fun tableOfContents_listsHeadingsWithStableIds() {
        val markdown =
            """
            # Intro

            Body

            ## Details

            ```
            # ignored
            ```
            """.trimIndent()

        assertEquals(
            listOf(
                TocEntry("intro", 1, "Intro"),
                TocEntry("details", 2, "Details"),
            ),
            MarkdownOutline.tableOfContents(markdown),
        )
    }
}
