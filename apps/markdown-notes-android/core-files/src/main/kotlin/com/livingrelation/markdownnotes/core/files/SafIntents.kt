package com.livingrelation.markdownnotes.core.files

import android.content.Intent

object SafIntents {
    private val markdownMimeTypes =
        arrayOf("text/markdown", "text/plain", "text/x-markdown")

    fun openDocumentIntent(): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            // */* so the system picker search can find .md files that are not tagged as markdown.
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, markdownMimeTypes + arrayOf("text/*", "*/*"))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }

    fun createDocumentIntent(suggestedTitle: String): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = "text/markdown"
            putExtra(Intent.EXTRA_TITLE, suggestedTitle)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }

    fun suggestedMarkdownTitle(baseName: String): String {
        val trimmed = baseName.trim().ifEmpty { "Untitled" }
        return if (trimmed.endsWith(".md", ignoreCase = true)) trimmed else "$trimmed.md"
    }
}
