package com.livingrelation.markdownnotes.core.files

data class RecentNote(
    val uriString: String,
    val displayName: String,
    val lastOpenedAtEpochMs: Long,
)
