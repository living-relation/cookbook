package com.livingrelation.markdownnotes.core.files

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NoteSessionState(
    val uri: Uri? = null,
    val displayName: String? = null,
    val isDirty: Boolean = false,
    val lastSavedAtEpochMs: Long? = null,
    val lastSaveError: FileError? = null,
)

class NoteSession(
    context: Context,
    scope: CoroutineScope,
    private val contentProvider: suspend () -> String,
    recentsRepository: RecentsRepository = DataStoreRecentsRepository.create(context),
) {
    private val appContext = context.applicationContext
    private val recents = recentsRepository
    private val drafts = DraftStorage(appContext)

    private val _state = MutableStateFlow(NoteSessionState())
    val state: StateFlow<NoteSessionState> = _state.asStateFlow()

    val autosave: AutosaveController =
        AutosaveController(
            scope = scope,
            context = appContext,
            draftStorage = drafts,
            isDirty = { _state.value.isDirty },
            targetUri = { _state.value.uri },
            contentToSave = contentProvider,
            onSaved = { savedAt, uri ->
                _state.value =
                    _state.value.copy(
                        isDirty = false,
                        lastSavedAtEpochMs = savedAt,
                        lastSaveError = null,
                    )
                if (uri == null) {
                    // Internal draft checkpoint only; recents unchanged.
                }
            },
            onSaveFailed = { error ->
                _state.value = _state.value.copy(lastSaveError = error, isDirty = true)
            },
        )

    suspend fun openFromPicker(uri: Uri): Result<String> {
        NoteIo.persistPermissions(appContext, uri)
        val readResult = NoteIo.readText(appContext, uri)
        return readResult.map { text ->
            val name = NoteIo.resolveDisplayName(appContext, uri)
            bindOpened(uri, name)
            recents.touch(uri, name)
            text
        }
    }

    suspend fun completeSaveAs(uri: Uri, markdown: String): Result<Unit> {
        NoteIo.persistPermissions(appContext, uri)
        return NoteIo.writeText(appContext, uri, markdown).map {
            val name = NoteIo.resolveDisplayName(appContext, uri)
            bindOpened(uri, name)
            recents.touch(uri, name)
            drafts.clearDraft()
            _state.value =
                _state.value.copy(
                    isDirty = false,
                    lastSavedAtEpochMs = System.currentTimeMillis(),
                    lastSaveError = null,
                )
        }
    }

    suspend fun saveNow(): Result<Unit> {
        val uri =
            _state.value.uri
                ?: return Result.failure(
                    FileIoException(FileError.NotFound),
                )
        val markdown = contentProvider()
        return NoteIo.writeText(appContext, uri, markdown).map {
            _state.value =
                _state.value.copy(
                    isDirty = false,
                    lastSavedAtEpochMs = System.currentTimeMillis(),
                    lastSaveError = null,
                )
        }
    }

    suspend fun openFromRecents(uri: Uri): Result<String> {
        if (!NoteIo.isUriAccessible(appContext, uri)) {
            recents.remove(uri)
            return Result.failure(FileIoException(FileError.AccessDenied))
        }
        return openFromPicker(uri)
    }

    suspend fun loadInternalDraft(): String? = drafts.readDraft()

    fun markDirty() {
        if (!_state.value.isDirty) {
            _state.value = _state.value.copy(isDirty = true)
        }
        autosave.schedule()
    }

    fun observeRecents() = recents.observeRecents()

    suspend fun pruneRecents() {
        recents.validateAndPrune(appContext)
    }

    suspend fun flushAutosave() {
        autosave.flush()
    }

    private fun bindOpened(uri: Uri, displayName: String) {
        _state.value =
            _state.value.copy(
                uri = uri,
                displayName = displayName,
                isDirty = false,
                lastSaveError = null,
            )
    }
}
