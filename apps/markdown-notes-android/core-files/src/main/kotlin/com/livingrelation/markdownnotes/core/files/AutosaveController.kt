package com.livingrelation.markdownnotes.core.files

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AutosaveController(
    private val scope: CoroutineScope,
    private val context: Context,
    private val draftStorage: DraftStorage,
    private val isDirty: () -> Boolean,
    private val targetUri: () -> Uri?,
    private val contentToSave: suspend () -> String,
    private val onSaved: suspend (savedAtEpochMs: Long, uri: Uri?) -> Unit,
    private val onSaveFailed: suspend (FileError) -> Unit,
) {
    private var pendingJob: Job? = null
    private var coalesceAnchorElapsedMs: Long? = null

    fun schedule() {
        if (!isDirty()) return
        val now = SystemClock.elapsedRealtime()
        if (coalesceAnchorElapsedMs == null) {
            coalesceAnchorElapsedMs = now
        }
        val elapsed = now - (coalesceAnchorElapsedMs ?: now)
        val delayMs =
            if (elapsed >= MAX_COALESCE_MS) {
                0L
            } else {
                DEBOUNCE_MS
            }
        pendingJob?.cancel()
        pendingJob =
            scope.launch {
                if (delayMs > 0L) delay(delayMs)
                performSaveIfDirty()
            }
    }

    suspend fun flush() {
        pendingJob?.cancel()
        pendingJob = null
        coalesceAnchorElapsedMs = null
        performSaveIfDirty()
    }

    private suspend fun performSaveIfDirty() {
        coalesceAnchorElapsedMs = null
        if (!isDirty()) return
        val markdown = contentToSave()
        val uri = targetUri()
        val result =
            if (uri != null) {
                NoteIo.writeText(context, uri, markdown)
            } else {
                draftStorage.writeDraft(markdown)
            }
        result.fold(
            onSuccess = {
                val savedAt = System.currentTimeMillis()
                onSaved(savedAt, uri)
            },
            onFailure = { throwable ->
                val error =
                    (throwable as? FileIoException)?.error
                        ?: FileError.ProviderError(throwable.message)
                onSaveFailed(error)
            },
        )
    }

    companion object {
        const val DEBOUNCE_MS = 800L
        const val MAX_COALESCE_MS = 5_000L
    }
}
