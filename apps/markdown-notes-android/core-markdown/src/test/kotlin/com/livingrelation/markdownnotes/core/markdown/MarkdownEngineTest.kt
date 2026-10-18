package com.livingrelation.markdownnotes.core.markdown

import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownEngineTest {
    private val engine = MarkdownEngine()

    @Test
    fun tableRoundTrip_containsPipeTable() {
        val input =
            """
            |# Title
            |
            || A | B |
            ||---|---|
            || 1 | 2 |
            """
                .trimMargin()
        val out = engine.roundTrip(input)
        assertTrue(out.contains("| A | B |"))
        assertTrue(out.contains("| 1 | 2 |"))
    }
}
