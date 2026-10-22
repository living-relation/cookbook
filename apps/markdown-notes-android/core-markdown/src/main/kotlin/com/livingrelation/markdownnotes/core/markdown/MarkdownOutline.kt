package com.livingrelation.markdownnotes.core.markdown

data class TocEntry(
    val id: String,
    val level: Int,
    val text: String,
)

object MarkdownOutline {
    private val headingLine = Regex("^(#{1,6})\\s+(.+?)\\s*#*\\s*$")
    private val fenceLine = Regex("^\\s*(```|~~~)")
    private val fenceLanguage = Regex("^\\s*(```|~~~)\\s*([A-Za-z0-9_+-]+)")

    val diagramLanguages = setOf("mermaid", "mmd", "chart")

    fun tableOfContents(markdown: String): List<TocEntry> {
        val usedIds = mutableMapOf<String, Int>()
        val entries = mutableListOf<TocEntry>()
        var inFence = false
        for (line in markdown.lineSequence()) {
            if (fenceLine.containsMatchIn(line)) {
                inFence = !inFence
                continue
            }
            if (inFence) continue
            val match = headingLine.matchEntire(line) ?: continue
            val text = match.groupValues[2].trim()
            if (text.isEmpty()) continue
            entries.add(
                TocEntry(
                    id = headingId(text, usedIds),
                    level = match.groupValues[1].length,
                    text = text,
                ),
            )
        }
        return entries
    }

    fun diagramFenceLanguages(markdown: String): List<String> {
        val languages = mutableListOf<String>()
        for (line in markdown.lineSequence()) {
            val match = fenceLanguage.find(line) ?: continue
            val language = match.groupValues[2].lowercase()
            if (language in diagramLanguages) {
                languages.add(language)
            }
        }
        return languages
    }

    internal fun headingId(text: String, usedIds: MutableMap<String, Int>): String {
        val base =
            text.lowercase()
                .trim()
                .replace(Regex("[^\\w\\s-]"), "")
                .replace(Regex("\\s+"), "-")
                .replace(Regex("-+"), "-")
                .trim('-')
                .ifEmpty { "section" }
        val count = (usedIds[base] ?: 0) + 1
        usedIds[base] = count
        return if (count == 1) base else "$base-$count"
    }
}
