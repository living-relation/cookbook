package com.livingrelation.markdownnotes.core.markdown

sealed interface Block {
    data class Heading(val level: Int, val text: String) : Block
    data class Paragraph(val text: String) : Block
    data class Table(val headerRow: Boolean, val rows: List<List<String>>) : Block
    data object HorizontalRule : Block
    data class CodeFence(val language: String?, val code: String) : Block
}

data class Document(val blocks: List<Block>)
