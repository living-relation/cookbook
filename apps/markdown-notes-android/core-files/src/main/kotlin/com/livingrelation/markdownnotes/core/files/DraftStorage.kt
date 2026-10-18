package com.livingrelation.markdownnotes.core.files

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DraftStorage(context: Context) {
    private val draftDir = File(context.applicationContext.filesDir, "drafts")
    private val draftFile = File(draftDir, "current.md")

    suspend fun readDraft(): String? =
        withContext(Dispatchers.IO) {
            if (!draftFile.isFile) return@withContext null
            draftFile.readText(Charsets.UTF_8)
        }

    suspend fun writeDraft(text: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                draftDir.mkdirs()
                draftFile.writeText(text, Charsets.UTF_8)
            }.recoverCatching { throwable ->
                throw FileIoException(
                    FileError.ProviderError(throwable.message ?: "Draft write failed"),
                )
            }
        }

    suspend fun clearDraft() {
        withContext(Dispatchers.IO) {
            if (draftFile.exists()) draftFile.delete()
        }
    }
}
