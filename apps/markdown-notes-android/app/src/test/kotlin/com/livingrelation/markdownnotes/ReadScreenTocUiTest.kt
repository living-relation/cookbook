package com.livingrelation.markdownnotes

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.livingrelation.markdownnotes.core.markdown.Block
import com.livingrelation.markdownnotes.core.markdown.MarkdownEngine
import com.livingrelation.markdownnotes.feature.reader.ReadScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ReadScreenTocUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tocDrawer_scrollsToSelectedHeading() {
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

        composeRule.setContent {
            MaterialTheme {
                ReadScreen(document = document)
            }
        }

        org.junit.Assert.assertTrue(jumpHeadingIndex >= 0)

        composeRule.onNodeWithTag("read-toc-open").performClick()
        composeRule.onNodeWithTag("read-toc-drawer").assertIsDisplayed()
        composeRule.onNodeWithTag("read-toc-item-$jumpHeadingIndex").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            try {
                composeRule.onNodeWithTag("read-heading-$jumpHeadingIndex").assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }
    }
}
