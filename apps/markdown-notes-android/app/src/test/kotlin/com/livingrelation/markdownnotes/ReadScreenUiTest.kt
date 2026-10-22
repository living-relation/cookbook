package com.livingrelation.markdownnotes

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.livingrelation.markdownnotes.core.files.NoteIo
import com.livingrelation.markdownnotes.feature.reader.ReadScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ReadScreenUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pinchZoom_andDoubleTapFitWidthReset() {
        composeRule.setContent {
            MaterialTheme {
                ReadScreen(
                    markdown = "# Welcome\n\nPinch to zoom this note.",
                    imageBaseUri = NoteIo.BUNDLED_NOTES_BASE_URI,
                )
            }
        }

        composeRule.onNodeWithTag("read-screen").assertIsDisplayed()
        composeRule.onNodeWithTag("read-screen").assertContentDescriptionContains("zoom 1.0")

        composeRule.onNodeWithTag("read-screen").performTouchInput {
            pinch(
                start0 = center,
                start1 = center,
                end0 = center.copy(x = center.x - 200f),
                end1 = center.copy(x = center.x + 200f),
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            val description = zoomDescription()
            description.contains("zoom") && !description.contains("zoom 1.0")
        }

        composeRule.onNodeWithTag("read-screen").performTouchInput {
            doubleClick(center)
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            zoomDescription().contains("zoom 1.0")
        }
    }

    private fun zoomDescription(): String =
        composeRule
            .onNodeWithTag("read-screen")
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.ContentDescription)
            ?.firstOrNull()
            .orEmpty()
}
