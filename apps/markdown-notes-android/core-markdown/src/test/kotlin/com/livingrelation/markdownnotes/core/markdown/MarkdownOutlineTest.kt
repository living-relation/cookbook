package com.livingrelation.markdownnotes.core.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownOutlineTest {
    @Test
    fun fixtureFiles_exist() {
        val names = FixtureNotes.allMarkdownFiles()
        assertTrue(names.contains("welcome.md"))
        assertTrue(names.contains("sample-table.md"))
        assertTrue(names.contains("sample-headings.md"))
        assertTrue(names.contains("sample-code-fence.md"))
        assertTrue(names.contains("sample-diagrams.md"))
    }

    @Test
    fun welcome_listsHeadingsAndDiagramFences() {
        val markdown = FixtureNotes.load("welcome.md")
        assertEquals(
            listOf("Welcome", "Flow", "Mix", "Conversation", "Picture", "Status"),
            MarkdownOutline.tableOfContents(markdown).map { it.text },
        )
        assertEquals(
            listOf("mermaid", "mermaid", "mermaid"),
            MarkdownOutline.diagramFenceLanguages(markdown),
        )
    }

    @Test
    fun headingsInsideFences_areIgnored() {
        val markdown =
            """
            # Visible

            ```
            # Not a heading
            ```

            ## Also visible
            """.trimIndent()
        assertEquals(
            listOf(
                TocEntry("visible", 1, "Visible"),
                TocEntry("also-visible", 2, "Also visible"),
            ),
            MarkdownOutline.tableOfContents(markdown),
        )
    }

    @Test
    fun duplicateHeadings_getUniqueIds() {
        val markdown =
            """
            # Intro
            # Intro
            """.trimIndent()
        assertEquals(
            listOf("intro", "intro-2"),
            MarkdownOutline.tableOfContents(markdown).map { it.id },
        )
    }

    @Test
    fun sampleHeadings_preserveLevels() {
        val toc = MarkdownOutline.tableOfContents(FixtureNotes.load("sample-headings.md"))
        assertEquals(listOf(1, 2, 3), toc.map { it.level })
        assertEquals(listOf("Level one", "Level two", "Level three"), toc.map { it.text })
    }

    @Test
    fun sampleDiagrams_detectsMermaidAndChartFences() {
        val markdown = FixtureNotes.load("sample-diagrams.md")
        assertEquals(
            listOf("mermaid", "chart"),
            MarkdownOutline.diagramFenceLanguages(markdown),
        )
    }
}
