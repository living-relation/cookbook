package com.livingrelation.markdownnotes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class MarkdownNotesUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launch_showsOpenFileControls() {
        composeRule.onNodeWithTag("open-file").assertIsDisplayed()
        composeRule.onNodeWithTag("open-recents").assertIsDisplayed()
        composeRule.onNodeWithTag("open-file-hero").assertIsDisplayed()
    }

    @Test
    fun readTab_showsWelcomeHeadingAndTable() {
        composeRule.onNodeWithText("Welcome").assertIsDisplayed()
        composeRule
            .onNodeWithText("Tap Open file at the top, then search for a .md file on your phone.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Feature  ·  Status").assertIsDisplayed()
        composeRule.onNodeWithText("Read  ·  Ready").assertIsDisplayed()
        composeRule.onNodeWithTag("read-screen").assertIsDisplayed()
    }

    @Test
    fun bottomNav_switchesEditAndTableThenBackToRead() {
        composeRule.onNodeWithTag("nav-edit").performClick()
        composeRule.onNodeWithTag("editor-screen").assertIsDisplayed()
        composeRule.onNodeWithTag("editor-mode-edit").assertIsDisplayed()

        composeRule.onNodeWithTag("nav-table").performClick()
        composeRule.onNodeWithTag("table-screen").assertIsDisplayed()
        composeRule.onNodeWithTag("table-cell-0-0").assertIsDisplayed()
        composeRule.onNodeWithText("Feature").assertIsDisplayed()

        composeRule.onNodeWithTag("nav-read").performClick()
        composeRule.onNodeWithText("Welcome").assertIsDisplayed()
    }

    @Test
    fun editTab_changingParagraphMarksDocumentDirtyInTitle() {
        composeRule.onNodeWithTag("nav-edit").performClick()
        composeRule.onNodeWithTag("toolbar-add-paragraph").performClick()
        composeRule.onNodeWithText("Markdown *").assertIsDisplayed()
    }

    @Test
    fun tableTab_editingCellMarksDocumentDirtyInTitle() {
        composeRule.onNodeWithTag("nav-table").performClick()
        composeRule
            .onNodeWithTag("table-cell-0-0")
            .performTextReplacement("Feature updated")
        composeRule.onNodeWithText("Markdown *").assertIsDisplayed()
    }

    @Test
    fun editTab_previewToggleShowsRenderedBlocks() {
        composeRule.onNodeWithTag("nav-edit").performClick()
        composeRule.onNodeWithTag("editor-mode-preview").performClick()
        composeRule.onNodeWithTag("editor-preview").assertIsDisplayed()
        composeRule.onNodeWithText("Welcome").assertIsDisplayed()
    }
}
