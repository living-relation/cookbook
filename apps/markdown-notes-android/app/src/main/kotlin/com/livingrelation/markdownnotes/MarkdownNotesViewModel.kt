package com.livingrelation.markdownnotes

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.livingrelation.markdownnotes.core.files.DataStoreRecentsRepository
import com.livingrelation.markdownnotes.core.files.FileError
import com.livingrelation.markdownnotes.core.files.FileIoException
import com.livingrelation.markdownnotes.core.files.NoteSession
import com.livingrelation.markdownnotes.core.files.NoteSessionState
import com.livingrelation.markdownnotes.core.files.RecentNote
import com.livingrelation.markdownnotes.core.files.RecentsRepository
import com.livingrelation.markdownnotes.core.markdown.Document
import com.livingrelation.markdownnotes.core.markdown.MarkdownEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MarkdownNotesViewModel(
    application: Application,
    private val engine: MarkdownEngine = MarkdownEngine(),
    recentsRepository: RecentsRepository = DataStoreRecentsRepository.create(application),
) : AndroidViewModel(application) {
    private val _markdown = MutableStateFlow(loadBundledWelcome(application))
    private val _fileMessage = MutableStateFlow<String?>(null)

    val fileMessage: StateFlow<String?> = _fileMessage.asStateFlow()

    val document: StateFlow<Document> =
        _markdown
            .map(engine::parse)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), engine.parse(_markdown.value))

    private val noteSession =
        NoteSession(
            context = application,
            scope = viewModelScope,
            contentProvider = { _markdown.value },
            recentsRepository = recentsRepository,
        )

    val sessionState: StateFlow<NoteSessionState> =
        noteSession.state.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            NoteSessionState(),
        )

    val recents =
        noteSession.observeRecents().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )

    init {
        viewModelScope.launch {
            noteSession.pruneRecents()
        }
    }

    fun clearFileMessage() {
        _fileMessage.value = null
    }

    fun openDocument(uri: Uri) {
        viewModelScope.launch {
            noteSession.openFromPicker(uri).fold(
                onSuccess = { text -> _markdown.value = text },
                onFailure = { throwable -> _fileMessage.value = describeFileFailure(throwable) },
            )
        }
    }

    fun openRecent(note: RecentNote) {
        val uri =
            runCatching { Uri.parse(note.uriString) }.getOrNull()
                ?: run {
                    _fileMessage.value = "Could not open recent note."
                    return
                }
        viewModelScope.launch {
            noteSession.openFromRecents(uri).fold(
                onSuccess = { text -> _markdown.value = text },
                onFailure = { throwable -> _fileMessage.value = describeFileFailure(throwable) },
            )
        }
    }

    fun onAppStop() {
        viewModelScope.launch {
            noteSession.flushAutosave()
        }
    }

    private fun describeFileFailure(throwable: Throwable): String {
        val error = (throwable as? FileIoException)?.error
        return when (error) {
            FileError.AccessDenied -> "Permission denied for this file."
            FileError.NotFound -> "File not found."
            FileError.EncodingError -> "Could not read file encoding (expected UTF-8)."
            is FileError.ProviderError ->
                error.detail?.let { "Could not open file: $it" } ?: "Could not open file."
            null -> throwable.message ?: "Could not open file."
        }
    }

    companion object {
        private fun loadBundledWelcome(application: Application): String {
            return runCatching {
                application.assets.open("notes/welcome.md").bufferedReader().use { it.readText() }
            }.getOrElse {
                """
                # Welcome

                Open a Markdown file to get started.
                """
                    .trimIndent()
            }
        }
    }
}
