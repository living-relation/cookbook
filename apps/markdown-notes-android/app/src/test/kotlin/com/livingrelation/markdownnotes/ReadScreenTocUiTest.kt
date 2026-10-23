package com.livingrelation.markdownnotes

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.livingrelation.markdownnotes.core.files.NoteIo
import com.livingrelation.markdownnotes.core.markdown.MarkdownOutline
import com.livingrelation.markdownnotes.feature.reader.ReadScreen
import org.junit.Assert.assertTrue
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
    fun tocDrawer_listsHeadingsAndClosesAfterPick() {
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
        val jump = MarkdownOutline.tableOfContents(markdown).first { it.text == "Jump here" }
        assertTrue(jump.id == "jump-here")

        composeRule.setContent {
            MaterialTheme {
                ReadScreen(
                    markdown = markdown,
                    imageBaseUri = NoteIo.BUNDLED_NOTES_BASE_URI,
                )
            }
        }

        composeRule.onNodeWithTag("read-toc-open").performClick()
        composeRule.onNodeWithTag("read-toc-drawer").assertIsDisplayed()
        composeRule.onNodeWithText("Jump here").assertIsDisplayed()
        composeRule.onNodeWithTag("read-toc-item-${jump.id}").performClick()
        composeRule.onNodeWithTag("read-toc-open").assertIsDisplayed()
    }
}
