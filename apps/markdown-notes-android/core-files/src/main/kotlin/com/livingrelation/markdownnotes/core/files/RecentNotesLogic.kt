package com.livingrelation.markdownnotes.core.files

object RecentNotesLogic {
    const val RECENTS_CAP = 20

    fun touch(
        current: List<RecentNote>,
        uriString: String,
        displayName: String,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): List<RecentNote> {
        val withoutDuplicate = current.filter { it.uriString != uriString }
        val entry = RecentNote(uriString, displayName, nowEpochMs)
        return (listOf(entry) + withoutDuplicate).take(RECENTS_CAP)
    }

    fun remove(current: List<RecentNote>, uriString: String): List<RecentNote> =
        current.filter { it.uriString != uriString }
}
