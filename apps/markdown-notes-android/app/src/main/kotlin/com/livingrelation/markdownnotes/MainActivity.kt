package com.livingrelation.markdownnotes

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.livingrelation.markdownnotes.core.files.RecentNote
import com.livingrelation.markdownnotes.core.files.SafIntents
import com.livingrelation.markdownnotes.feature.editor.EditorScreen
import com.livingrelation.markdownnotes.feature.reader.ReadScreen
import com.livingrelation.markdownnotes.feature.table.TableEditorScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MarkdownViewerApp(initialUri = intent.data)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

private enum class MainDestination(val route: String, val label: String) {
    Read("read", "View"),
    Edit("edit", "Edit"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MarkdownViewerApp(initialUri: Uri?) {
    val viewModel: MarkdownNotesViewModel = viewModel()
    val document by viewModel.document.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val recents by viewModel.recents.collectAsStateWithLifecycle()
    val fileMessage by viewModel.fileMessage.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val destinations = MainDestination.entries
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val selectedIndex =
        destinations.indexOfFirst { it.route == currentRoute }.let { index ->
            if (index >= 0) index else 0
        }

    val snackbarHostState = remember { SnackbarHostState() }
    var showRecents by remember { mutableStateOf(false) }
    var showTableEditor by remember { mutableStateOf(false) }
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

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) {
                    viewModel.onAppStop()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showTableEditor) {
        BackHandler { showTableEditor = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = sessionState.displayName ?: "Markdown"
                    Text(if (sessionState.isDirty) "$title *" else title)
                },
                actions = {
                    TextButton(
                        onClick = { showTableEditor = true },
                        modifier = Modifier.testTag("nav-table"),
                    ) {
                        Text("Tables")
                    }
                    TextButton(
                        onClick = { showRecents = true },
                        modifier = Modifier.testTag("open-recents"),
                    ) {
                        Text("Recents")
                    }
                    Button(
                        onClick = { openFilePicker() },
                        modifier = Modifier.testTag("open-file"),
                    ) {
                        Text("Open file")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                destinations.forEachIndexed { index, destination ->
                    NavigationBarItem(
                        modifier = Modifier.testTag("nav-${destination.route}"),
                        selected = selectedIndex == index && !showTableEditor,
                        onClick = {
                            showTableEditor = false
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Text(destination.label.first().toString()) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        if (showTableEditor) {
            TableEditorScreen(
                document = document,
                onDocumentChange = viewModel::applyDocument,
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            NavHost(
                navController = navController,
                startDestination = MainDestination.Read.route,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(MainDestination.Read.route) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (sessionState.uri == null) {
                            OpenFileHero(
                                onOpenFile = { openFilePicker() },
                                onRecents = { showRecents = true },
                            )
                        }
                        ReadScreen(
                            document = document,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                composable(MainDestination.Edit.route) {
                    EditorScreen(
                        document = document,
                        onDocumentChange = viewModel::applyDocument,
                    )
                }
            }
        }
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

@Composable
private fun OpenFileHero(
    onOpenFile: () -> Unit,
    onRecents: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("open-file-hero"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Open a markdown file",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = "Tap Open file, then use your phone’s search to find a .md file.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            onClick = onOpenFile,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag("open-file-hero-button"),
        ) {
            Text("Open file")
        }
        TextButton(onClick = onRecents, modifier = Modifier.fillMaxWidth()) {
            Text("Recent files")
        }
    }
}

@Composable
private fun RecentsSheet(
    recents: List<RecentNote>,
    onRecentSelected: (RecentNote) -> Unit,
    onOpenFile: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered =
        remember(recents, query) {
            val needle = query.trim()
            if (needle.isEmpty()) {
                recents
            } else {
                recents.filter { it.displayName.contains(needle, ignoreCase = true) }
            }
        }
    Column(modifier = Modifier.padding(bottom = 32.dp)) {
        Text(
            text = "Find a markdown file",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        Button(
            onClick = onOpenFile,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .testTag("recents-open-file"),
        ) {
            Text("Search files on this phone")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .testTag("recents-search"),
            label = { Text("Filter recents") },
            singleLine = true,
        )
        if (filtered.isEmpty()) {
            Text(
                text = "No matches yet. Use Search files on this phone.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
        } else {
            LazyColumn {
                items(filtered, key = { it.uriString }) { note ->
                    Text(
                        text = note.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { onRecentSelected(note) }
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}
