package com.livingrelation.markdownnotes.core.markdown

import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TableBody
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.ext.gfm.tables.TableHead
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.Heading
import org.commonmark.node.Node
import org.commonmark.node.Paragraph
import org.commonmark.node.Text
import org.commonmark.node.ThematicBreak
import org.commonmark.parser.Parser
class MarkdownEngine {
    private val parser: Parser =
        Parser.builder().extensions(listOf(TablesExtension.create())).build()

    fun parse(markdown: String): Document {
        val root = parser.parse(markdown.trim())
        val blocks = mutableListOf<Block>()
        var node: Node? = root.firstChild
        while (node != null) {
            when (node) {
                is Heading ->
                    blocks.add(Block.Heading(node.level, inlineText(node)))
                is Paragraph ->
                    blocks.add(Block.Paragraph(inlineText(node)))
                is FencedCodeBlock ->
                    blocks.add(
                        Block.CodeFence(
                            language = node.info.takeIf { it.isNotBlank() },
                            code = node.literal.orEmpty().trimEnd(),
                        ),
                    )
                is ThematicBreak -> blocks.add(Block.HorizontalRule)
                is TableBlock -> {
                    val rows = mutableListOf<List<String>>()
                    var section: Node? = node.firstChild
                    while (section != null) {
                        if (section is TableHead || section is TableBody) {
                            var rowNode: Node? = section.firstChild
                            while (rowNode != null) {
                                if (rowNode is TableRow) {
                                    rows.add(tableRowCells(rowNode))
                                }
                                rowNode = rowNode.next
                            }
                        }
                        section = section.next
                    }
                    blocks.add(Block.Table(headerRow = true, rows = rows))
                }
            }
            node = node.next
        }
        return Document(blocks)
    }

    fun emit(document: Document): String {
        val sb = StringBuilder()
        for (block in document.blocks) {
            when (block) {
                is Block.Heading -> {
                    sb.append("#".repeat(block.level.coerceIn(1, 3)))
                    sb.append(' ')
                    sb.appendLine(block.text)
                    sb.appendLine()
                }
                is Block.Paragraph -> {
                    sb.appendLine(block.text)
                    sb.appendLine()
                }
                is Block.CodeFence -> {
                    sb.append("```")
                    if (!block.language.isNullOrBlank()) sb.append(block.language)
                    sb.appendLine()
                    sb.appendLine(block.code.trimEnd())
                    sb.appendLine("```")
                    sb.appendLine()
                }
                Block.HorizontalRule -> {
                    sb.appendLine("---")
                    sb.appendLine()
                }
                is Block.Table -> {
                    if (block.rows.isEmpty()) continue
                    val columnCount = block.rows.maxOf { it.size }
                    fun pad(row: List<String>): List<String> =
                        row + List(columnCount - row.size) { "" }
                    val rows = block.rows.map(::pad)
                    sb.appendLine("| " + rows.first().joinToString(" | ", transform = ::escapeTableCell) + " |")
                    sb.appendLine("| " + List(columnCount) { "---" }.joinToString(" | ") + " |")
                    for (r in rows.drop(1)) {
                        sb.appendLine("| " + r.joinToString(" | ", transform = ::escapeTableCell) + " |")
                    }
                    sb.appendLine()
                }
            }
        }
        return sb.toString().trimEnd() + "\n"
    }

    fun roundTrip(markdown: String): String = emit(parse(markdown))

    private fun tableRowCells(row: TableRow): List<String> {
        val cells = mutableListOf<String>()
        var cellNode: Node? = row.firstChild
        while (cellNode != null) {
            if (cellNode is TableCell) {
                cells.add(inlineText(cellNode))
            }
            cellNode = cellNode.next
        }
        return cells
    }

    private fun escapeTableCell(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            when (ch) {
                '\\' -> sb.append("\\\\")
                '|' -> sb.append("\\|")
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun inlineText(node: Node): String {
        val sb = StringBuilder()
        var child: Node? = node.firstChild
        while (child != null) {
            if (child is Text) sb.append(child.literal)
            else sb.append(inlineText(child))
            child = child.next
        }
        return sb.toString()
    }
}
