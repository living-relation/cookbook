package com.livingrelation.markdownnotes.feature.reader

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.livingrelation.markdownnotes.core.markdown.Block
import com.livingrelation.markdownnotes.core.markdown.Document

@Composable
fun ReadScreen(document: Document, modifier: Modifier = Modifier) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val transformState =
        rememberTransformableState { zoomChange, panChange, _ ->
            scale = (scale * zoomChange).coerceIn(0.8f, 3f)
            if (scale > 1f) {
                offsetX += panChange.x
                offsetY += panChange.y
            } else {
                offsetX = 0f
                offsetY = 0f
            }
        }

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
                .transformable(transformState)
                .verticalScroll(rememberScrollState())
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
