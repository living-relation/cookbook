package com.livingrelation.markdownnotes.feature.reader

import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                is Block.Paragraph ->
                    Text(
                        text = block.text,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                is Block.CodeFence ->
                    Text(
                        text = block.code,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                Block.HorizontalRule ->
                    Text("—", modifier = Modifier.padding(vertical = 12.dp))
                is Block.Table -> {
                    for (row in block.rows) {
                        Text(
                            text = row.joinToString("  ·  "),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}
