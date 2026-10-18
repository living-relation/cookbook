package com.livingrelation.markdownnotes.feature.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.livingrelation.markdownnotes.core.markdown.Block
import com.livingrelation.markdownnotes.core.markdown.Document

private const val MIN_ZOOM_SCALE = 0.8f
private const val MAX_ZOOM_SCALE = 3f
private const val FIT_WIDTH_SCALE = 1f
private const val ZOOMED_SCALE_EPSILON = 0.01f

@Composable
fun ReadScreen(document: Document, modifier: Modifier = Modifier) {
    var scale by remember { mutableFloatStateOf(FIT_WIDTH_SCALE) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    fun resetToFitWidth() {
        scale = FIT_WIDTH_SCALE
        offsetX = 0f
        offsetY = 0f
    }

    val transformState =
        rememberTransformableState { zoomChange, panChange, _ ->
            val newScale = (scale * zoomChange).coerceIn(MIN_ZOOM_SCALE, MAX_ZOOM_SCALE)
            scale = newScale
            if (newScale > FIT_WIDTH_SCALE + ZOOMED_SCALE_EPSILON) {
                offsetX += panChange.x
                offsetY += panChange.y
            } else {
                offsetX = 0f
                offsetY = 0f
            }
        }

    val scrollState = rememberScrollState()
    val isZoomedIn = scale > FIT_WIDTH_SCALE + ZOOMED_SCALE_EPSILON

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .testTag("read-screen")
                .semantics { contentDescription = "zoom ${"%.1f".format(scale)}" }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { resetToFitWidth() },
                    )
                }
                .transformable(
                    state = transformState,
                    lockRotationOnZoomPan = true,
                )
                .verticalScroll(scrollState, enabled = !isZoomedIn)
                .padding(20.dp),
    ) {
        for (block in document.blocks) {
            BlockView(block = block)
        }
    }
}

@Composable
private fun BlockView(block: Block, modifier: Modifier = Modifier) {
    when (block) {
        is Block.Heading ->
            Text(
                text = block.text,
                style =
                    when (block.level) {
                        1 -> MaterialTheme.typography.headlineLarge
                        2 -> MaterialTheme.typography.headlineMedium
                        else -> MaterialTheme.typography.headlineSmall
                    },
                modifier = modifier.padding(vertical = 8.dp),
            )
        is Block.Paragraph ->
            Text(
                text = block.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = modifier.padding(vertical = 4.dp),
            )
        is Block.CodeFence ->
            Text(
                text = block.code,
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                modifier =
                    modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp),
            )
        Block.HorizontalRule ->
            HorizontalDivider(modifier = modifier.padding(vertical = 12.dp))
        is Block.Table -> {
            Column(modifier = modifier.padding(vertical = 8.dp)) {
                block.rows.forEachIndexed { index, row ->
                    val isHeader = block.headerRow && index == 0
                    Text(
                        text = row.joinToString("  ·  "),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
        }
    }
}
