package com.livingrelation.markdownnotes.core.files

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentNotesLogicTest {
    @Test
    fun touch_movesExistingEntryToFront() {
        val initial =
            listOf(
                RecentNote("content://a", "a.md", 1L),
                RecentNote("content://b", "b.md", 2L),
            )
        val updated =
            RecentNotesLogic.touch(
                current = initial,
                uriString = "content://b",
                displayName = "b-renamed.md",
                nowEpochMs = 99L,
            )
        assertEquals("content://b", updated.first().uriString)
        assertEquals("b-renamed.md", updated.first().displayName)
        assertEquals(99L, updated.first().lastOpenedAtEpochMs)
        assertEquals(2, updated.size)
    }

    @Test
    fun touch_capsAtTwenty() {
        val initial =
            (0 until RecentNotesLogic.RECENTS_CAP).map { index ->
                RecentNote("content://$index", "n$index.md", index.toLong())
            }
        val updated =
            RecentNotesLogic.touch(
                current = initial,
                uriString = "content://new",
                displayName = "new.md",
                nowEpochMs = 100L,
            )
        assertEquals(RecentNotesLogic.RECENTS_CAP, updated.size)
        assertEquals("content://new", updated.first().uriString)
        assertTrue(updated.none { it.uriString == "content://${RecentNotesLogic.RECENTS_CAP - 1}" })
    }

    @Test
    fun remove_dropsMatchingUri() {
        val initial =
            listOf(
                RecentNote("content://a", "a.md", 1L),
                RecentNote("content://b", "b.md", 2L),
            )
        val updated = RecentNotesLogic.remove(initial, "content://a")
        assertEquals(1, updated.size)
        assertEquals("content://b", updated.single().uriString)
    }
}
