package com.livingrelation.markdownnotes.core.files

import android.content.Intent

object SafIntents {
    private val markdownMimeTypes =
        arrayOf("text/markdown", "text/plain", "text/x-markdown")

    fun openDocumentIntent(): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, markdownMimeTypes + arrayOf("text/*", "*/*"))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
}
