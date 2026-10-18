package com.livingrelation.markdownnotes.feature.reader

import com.livingrelation.markdownnotes.core.markdown.Block
import com.livingrelation.markdownnotes.core.markdown.MarkdownEngine
import org.junit.Assert.assertTrue
import org.junit.Test

class TocScrollFixtureTest {
    @Test
    fun longNote_containsJumpHeading() {
        val markdown =
            buildString {
                appendLine("# Top")
                appendLine()
                for (index in 1..40) {
                    appendLine("Filler paragraph $index with enough text to scroll.")
                    appendLine()
                }
                appendLine("## Jump here")
                appendLine()
                appendLine("More content at the bottom.")
            }
        val document = MarkdownEngine().parse(markdown)
        val jumpHeadingIndex =
            document.blocks.indexOfFirst { block ->
                block is Block.Heading && block.text == "Jump here"
            }
        assertTrue(jumpHeadingIndex >= 0)
    }
}
