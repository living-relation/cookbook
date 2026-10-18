package com.livingrelation.markdownnotes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class MarkdownNotesInstrumentedUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launch_showsWelcomeAndCanOpenTabs() {
        composeRule.onNodeWithText("Welcome").assertIsDisplayed()
        composeRule.onNodeWithTag("nav-edit").performClick()
        composeRule.onNodeWithTag("editor-screen").assertIsDisplayed()
        composeRule.onNodeWithTag("nav-read").performClick()
        composeRule.onNodeWithText("Welcome").assertIsDisplayed()
    }
}
