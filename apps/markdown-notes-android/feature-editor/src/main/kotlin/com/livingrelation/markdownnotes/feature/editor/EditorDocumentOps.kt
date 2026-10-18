package com.livingrelation.markdownnotes.feature.editor

import com.livingrelation.markdownnotes.core.markdown.Block
import com.livingrelation.markdownnotes.core.markdown.Document

enum class InsertableBlockType {
    Paragraph,
    Heading,
    CodeFence,
    HorizontalRule,
}

fun Document.updateBlock(index: Int, block: Block): Document {
    if (index !in blocks.indices) return this
    return copy(blocks = blocks.toMutableList().also { it[index] = block })
}

fun Document.removeBlock(index: Int): Document {
    if (index !in blocks.indices) return this
    return copy(blocks = blocks.filterIndexed { i, _ -> i != index })
}

fun Document.insertBlockAfter(index: Int?, type: InsertableBlockType): Document {
    val newBlock =
        when (type) {
            InsertableBlockType.Paragraph -> Block.Paragraph("")
            InsertableBlockType.Heading -> Block.Heading(level = 1, text = "")
            InsertableBlockType.CodeFence -> Block.CodeFence(language = null, code = "")
            InsertableBlockType.HorizontalRule -> Block.HorizontalRule
        }
    val insertAt =
        when {
            blocks.isEmpty() -> 0
            index == null -> blocks.size
            index < 0 -> 0
            index >= blocks.lastIndex -> blocks.size
            else -> index + 1
        }
    val updated = blocks.toMutableList()
    updated.add(insertAt, newBlock)
    return copy(blocks = updated)
}

fun Document.moveBlockUp(index: Int): Document {
    if (index <= 0 || index >= blocks.size) return this
    val updated = blocks.toMutableList()
    val block = updated.removeAt(index)
    updated.add(index - 1, block)
    return copy(blocks = updated)
}

fun Document.moveBlockDown(index: Int): Document {
    if (index < 0 || index >= blocks.lastIndex) return this
    val updated = blocks.toMutableList()
    val block = updated.removeAt(index)
    updated.add(index + 1, block)
    return copy(blocks = updated)
}
