package com.livingrelation.markdownnotes

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livingrelation.markdownnotes.core.files.NoteIo
import com.livingrelation.markdownnotes.core.files.SafIntents
import com.livingrelation.markdownnotes.feature.reader.ReadScreen
import com.livingrelation.markdownnotes.ui.MarkdownTheme
import com.livingrelation.markdownnotes.ui.RecentsSheet
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var incomingUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingUri = intent.data
        setContent {
            MarkdownTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MarkdownViewerApp(initialUri = incomingUri)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingUri = intent.data
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MarkdownViewerApp(initialUri: Uri?) {
    val viewModel: MarkdownNotesViewModel = viewModel()
    val markdown by viewModel.markdown.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val recents by viewModel.recents.collectAsStateWithLifecycle()
    val fileMessage by viewModel.fileMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showRecents by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val openLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let(viewModel::openDocument)
            }
        }

    fun openFilePicker() {
        openLauncher.launch(SafIntents.openDocumentIntent())
    }

    LaunchedEffect(initialUri) {
        initialUri?.let(viewModel::openDocument)
    }

    LaunchedEffect(fileMessage) {
        val message = fileMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearFileMessage()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = sessionState.displayName ?: "Markdown",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showRecents = true },
                        modifier = Modifier.testTag("open-recents"),
                    ) {
                        Icon(Icons.Filled.History, contentDescription = "Recents")
                    }
                    Button(
                        onClick = { openFilePicker() },
                        modifier = Modifier.testTag("open-file"),
                    ) {
                        Icon(
                            Icons.Filled.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 6.dp),
                        )
                        Text("Open")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        ReadScreen(
            markdown = markdown,
            imageBaseUri = sessionState.imageBaseUri ?: NoteIo.BUNDLED_NOTES_BASE_URI,
            modifier = Modifier.padding(innerPadding),
        )
    }

    if (showRecents) {
        ModalBottomSheet(
            onDismissRequest = { showRecents = false },
            sheetState = sheetState,
        ) {
            RecentsSheet(
                recents = recents,
                onRecentSelected = { note ->
                    viewModel.openRecent(note)
                    scope.launch {
                        sheetState.hide()
                        showRecents = false
                    }
                },
                onOpenFile = {
                    scope.launch {
                        sheetState.hide()
                        showRecents = false
                        openFilePicker()
                    }
                },
            )
        }
    }
}
