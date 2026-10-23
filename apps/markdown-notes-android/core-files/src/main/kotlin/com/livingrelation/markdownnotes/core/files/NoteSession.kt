package com.livingrelation.markdownnotes.core.files

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NoteSessionState(
    val uri: Uri? = null,
    val displayName: String? = null,
    val imageBaseUri: String? = NoteIo.BUNDLED_NOTES_BASE_URI,
)

class NoteSession(
    context: Context,
    recentsRepository: RecentsRepository = DataStoreRecentsRepository.create(context),
) {
    private val appContext = context.applicationContext
    private val recents = recentsRepository

    private val _state = MutableStateFlow(NoteSessionState())
    val state: StateFlow<NoteSessionState> = _state.asStateFlow()

    suspend fun open(uri: Uri): Result<String> {
        NoteIo.persistReadPermission(appContext, uri)
        return NoteIo.readText(appContext, uri).map { text ->
            val name = NoteIo.resolveDisplayName(appContext, uri)
            _state.value =
                NoteSessionState(
                    uri = uri,
                    displayName = name,
                    imageBaseUri = NoteIo.imageBaseUri(uri),
                )
            recents.touch(uri, name)
            text
        }
    }

    suspend fun openFromRecents(uri: Uri): Result<String> {
        if (!NoteIo.isUriAccessible(appContext, uri)) {
            recents.remove(uri)
            return Result.failure(FileIoException(FileError.AccessDenied))
        }
        return open(uri)
    }

    fun observeRecents() = recents.observeRecents()
}
