package com.livingrelation.markdownnotes.core.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownEngineTest {
    private val engine = MarkdownEngine()

    @Test
    fun fixtureFiles_exist() {
        val names = FixtureNotes.allMarkdownFiles()
        assertTrue(names.contains("welcome.md"))
        assertTrue(names.contains("sample-table.md"))
        assertTrue(names.contains("sample-headings.md"))
        assertTrue(names.contains("sample-code-fence.md"))
    }

    @Test
    fun welcome_parsesExpectedStructure() {
        val doc = engine.parse(FixtureNotes.load("welcome.md"))
        assertEquals(
            listOf(
                Block.Heading(1, "Welcome"),
                Block.Paragraph("This app was built by an automated pipeline."),
                Block.Table(
                    headerRow = true,
                    rows =
                        listOf(
                            listOf("Feature", "Status"),
                            listOf("Read", "Ready"),
                            listOf("Edit", "In progress"),
                        ),
                ),
            ),
            doc.blocks,
        )
    }

    @Test
    fun sampleTable_parsesExpectedStructure() {
        val doc = engine.parse(FixtureNotes.load("sample-table.md"))
        assertEquals(
            listOf(
                Block.Heading(1, "Weekly plan"),
                Block.Table(
                    headerRow = true,
                    rows =
                        listOf(
                            listOf("Day", "Focus"),
                            listOf("Mon", "Reading"),
                            listOf("Tue", "Tables"),
                        ),
                ),
            ),
            doc.blocks,
        )
    }

    @Test
    fun sampleHeadings_parsesExpectedStructure() {
        val doc = engine.parse(FixtureNotes.load("sample-headings.md"))
        assertEquals(
            listOf(
                Block.Heading(1, "Level one"),
                Block.Heading(2, "Level two"),
                Block.Heading(3, "Level three"),
                Block.Paragraph("Body text under the headings."),
                Block.HorizontalRule,
                Block.Paragraph("Horizontal rule above was parsed as a block."),
            ),
            doc.blocks,
        )
    }

    @Test
    fun sampleCodeFence_parsesExpectedStructure() {
        val doc = engine.parse(FixtureNotes.load("sample-code-fence.md"))
        assertEquals(
            listOf(
                Block.Heading(1, "Snippet"),
                Block.CodeFence(
                    language = "kotlin",
                    code = "fun greet(name: String) = \"Hello, \$name\"",
                ),
                Block.Paragraph("Use fences for short examples in notes."),
            ),
            doc.blocks,
        )
    }

    @Test
    fun allFixtures_semanticRoundTripIdempotent() {
        for (name in FixtureNotes.allMarkdownFiles()) {
            val source = FixtureNotes.load(name)
            val once = engine.parse(source)
            val twice = engine.parse(engine.emit(once))
            assertEquals("parse(emit(parse($name)))", once, twice)
        }
    }

    @Test
    fun allFixtures_roundTripPreservesStructure() {
        for (name in FixtureNotes.allMarkdownFiles()) {
            val source = FixtureNotes.load(name)
            val parsed = engine.parse(source)
            val roundTripped = engine.parse(engine.roundTrip(source))
            assertEquals("roundTrip structure for $name", parsed, roundTripped)
        }
    }

    @Test
    fun editorOriginDocument_parseEmitEquals() {
        val built =
            Document(
                blocks =
                    listOf(
                        Block.Heading(2, "Note"),
                        Block.Paragraph("Plain body"),
                        Block.Table(
                            headerRow = true,
                            rows = listOf(listOf("A", "B|C"), listOf("1", "2")),
                        ),
                        Block.CodeFence(language = "text", code = "line1\nline2"),
                        Block.HorizontalRule,
                    ),
            )
        assertEquals(built, engine.parse(engine.emit(built)))
    }

    @Test
    fun tableEmit_escapesPipesInCells() {
        val doc =
            Document(
                blocks =
                    listOf(
                        Block.Table(
                            headerRow = true,
                            rows = listOf(listOf("x|y", "z"), listOf("a", "b")),
                        ),
                    ),
            )
        val out = engine.emit(doc)
        assertTrue(out.contains("x\\|y"))
        assertEquals(doc, engine.parse(out))
    }

    @Test
    fun tableRoundTrip_raggedRowsPadded() {
        val input =
            """
            |# Title
            |
            || A | B |
            || --- | --- |
            || 1 | 2 |
            """
                .trimMargin()
        val out = engine.roundTrip(input)
        assertTrue(out.contains("| A | B |"))
        assertTrue(out.contains("| 1 | 2 |"))
    }
}
