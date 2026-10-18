package com.livingrelation.markdownnotes.feature.reader

import com.livingrelation.markdownnotes.core.markdown.Block
import com.livingrelation.markdownnotes.core.markdown.Document

data class TocEntry(
    val blockIndex: Int,
    val level: Int,
    val text: String,
)

fun Document.tableOfContents(): List<TocEntry> =
    blocks.mapIndexedNotNull { index, block ->
        when (block) {
            is Block.Heading ->
                TocEntry(
                    blockIndex = index,
                    level = block.level,
                    text = block.text,
                )
            is Block.Paragraph,
            is Block.CodeFence,
            Block.HorizontalRule,
            is Block.Table,
            -> null
        }
    }
