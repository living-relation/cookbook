package com.livingrelation.markdownnotes.feature.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.livingrelation.markdownnotes.core.markdown.Block

@Composable
fun MarkdownTable(
    table: Block.Table,
    modifier: Modifier = Modifier,
    cellWidth: Dp = 140.dp,
) {
    val rows = if (table.rows.isEmpty()) listOf(listOf("")) else table.rows
    val columnCount = rows.maxOf { it.size }.coerceAtLeast(1)
    val shape = RoundedCornerShape(10.dp)
    val border = MaterialTheme.colorScheme.outline
    Column(
        modifier =
            modifier
                .horizontalScroll(rememberScrollState())
                .clip(shape)
                .border(1.dp, border, shape),
    ) {
        rows.forEachIndexed { rowIndex, row ->
            val isHeader = table.headerRow && rowIndex == 0
            val stripe = !isHeader && rowIndex % 2 == 1
            Row {
                repeat(columnCount) { columnIndex ->
                    val value = row.getOrElse(columnIndex) { "" }
                    Text(
                        text = value.ifBlank { " " },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal,
                        modifier =
                            Modifier
                                .width(cellWidth)
                                .background(
                                    when {
                                        isHeader -> MaterialTheme.colorScheme.secondaryContainer
                                        stripe -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                        else -> MaterialTheme.colorScheme.surface
                                    },
                                )
                                .border(0.5.dp, border.copy(alpha = 0.7f))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}
