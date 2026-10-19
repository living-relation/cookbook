package com.livingrelation.markdownnotes.feature.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.livingrelation.markdownnotes.core.markdown.Block
import com.livingrelation.markdownnotes.core.markdown.Document
import kotlinx.coroutines.launch

private const val MIN_ZOOM_SCALE = 0.8f
private const val MAX_ZOOM_SCALE = 3f
private const val FIT_WIDTH_SCALE = 1f
private const val ZOOMED_SCALE_EPSILON = 0.01f

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReadScreen(document: Document, modifier: Modifier = Modifier) {
    val toc = remember(document) { document.tableOfContents() }
    var showTocDrawer by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val bringIntoViewRequesters =
        remember(document) {
            List(document.blocks.size) { BringIntoViewRequester() }
        }

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

    val isZoomedIn = scale > FIT_WIDTH_SCALE + ZOOMED_SCALE_EPSILON

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
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
                    .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 88.dp),
        ) {
            document.blocks.forEachIndexed { index, block ->
                BlockView(
                    block = block,
                    blockIndex = index,
                    modifier = Modifier.bringIntoViewRequester(bringIntoViewRequesters[index]),
                )
            }
        }
        if (toc.isNotEmpty()) {
            FloatingActionButton(
                onClick = { showTocDrawer = true },
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .testTag("read-toc-open"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = "Table of contents",
                )
            }
        }
        if (showTocDrawer) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clickable(onClick = { showTocDrawer = false })
                        .background(Color.Black.copy(alpha = 0.32f))
                        .testTag("read-toc-scrim"),
            )
            Surface(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .widthIn(max = 320.dp)
                        .fillMaxWidth(0.86f)
                        .align(Alignment.CenterEnd)
                        .testTag("read-toc-drawer"),
                tonalElevation = 3.dp,
            ) {
                Column(
                    modifier =
                        Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 24.dp),
                ) {
                    Text(
                        text = "Contents",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    )
                    toc.forEach { entry ->
                        TextButton(
                            onClick = {
                                showTocDrawer = false
                                scope.launch {
                                    bringIntoViewRequesters
                                        .getOrNull(entry.blockIndex)
                                        ?.bringIntoView()
                                }
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .testTag("read-toc-item-${entry.blockIndex}")
                                    .padding(start = ((entry.level - 1).coerceAtLeast(0) * 16).dp),
                        ) {
                            Text(
                                text = entry.text,
                                style =
                                    when (entry.level) {
                                        1 -> MaterialTheme.typography.titleMedium
                                        2 -> MaterialTheme.typography.titleSmall
                                        else -> MaterialTheme.typography.bodyLarge
                                    },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockView(
    block: Block,
    blockIndex: Int,
    modifier: Modifier = Modifier,
) {
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
                modifier =
                    modifier
                        .padding(top = 20.dp, bottom = 8.dp)
                        .testTag("read-heading-$blockIndex"),
            )
        is Block.Paragraph ->
            Text(
                text = block.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = modifier.padding(vertical = 6.dp),
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
        is Block.Table ->
            MarkdownTable(
                table = block,
                modifier = modifier.padding(vertical = 12.dp),
            )
    }
}
