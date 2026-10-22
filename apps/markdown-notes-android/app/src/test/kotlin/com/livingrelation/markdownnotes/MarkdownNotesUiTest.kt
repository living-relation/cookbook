package com.livingrelation.markdownnotes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
    fun launch_showsOpenFileControlsAndReader() {
        composeRule.onNodeWithTag("open-file").assertIsDisplayed()
        composeRule.onNodeWithTag("open-recents").assertIsDisplayed()
        composeRule.onNodeWithTag("read-screen").assertIsDisplayed()
        composeRule.onNodeWithText("Markdown").assertIsDisplayed()
    }
}
