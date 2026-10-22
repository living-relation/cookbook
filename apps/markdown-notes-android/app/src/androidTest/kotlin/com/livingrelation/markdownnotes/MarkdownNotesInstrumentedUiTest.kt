package com.livingrelation.markdownnotes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test

class MarkdownNotesInstrumentedUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launch_showsReaderAndOpenControls() {
        composeRule.onNodeWithTag("read-screen").assertIsDisplayed()
        composeRule.onNodeWithTag("open-file").assertIsDisplayed()
        composeRule.onNodeWithTag("open-recents").assertIsDisplayed()
    }
}
