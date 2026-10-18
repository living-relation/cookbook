package com.livingrelation.markdownnotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.livingrelation.markdownnotes.core.markdown.Document
import com.livingrelation.markdownnotes.core.markdown.MarkdownEngine
import com.livingrelation.markdownnotes.feature.editor.EditorPlaceholder
import com.livingrelation.markdownnotes.feature.reader.ReadScreen
import com.livingrelation.markdownnotes.feature.table.TableEditorPlaceholder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sample =
            """
            # Welcome

            This app was built by an automated pipeline.

            | Feature | Status |
            | --- | --- |
            | Read | Ready |
            | Edit | In progress |
            """
                .trimIndent()
        val document = MarkdownEngine().parse(sample)
        setContent {
            MaterialTheme {
                Surface {
                    MarkdownNotesApp(document = document)
                }
            }
        }
    }
}

private enum class MainDestination(val route: String, val label: String) {
    Read("read", "Read"),
    Edit("edit", "Edit"),
    Table("table", "Table"),
}

@Composable
private fun MarkdownNotesApp(document: Document) {
    val navController = rememberNavController()
    val destinations = MainDestination.entries
    var selectedIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                destinations.forEachIndexed { index, destination ->
                    NavigationBarItem(
                        modifier = Modifier.testTag("nav-${destination.route}"),
                        selected = selectedIndex == index,
                        onClick = {
                            selectedIndex = index
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
        NavHost(
            navController = navController,
            startDestination = MainDestination.Read.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(MainDestination.Read.route) {
                ReadScreen(document = document)
            }
            composable(MainDestination.Edit.route) {
                EditorPlaceholder()
            }
            composable(MainDestination.Table.route) {
                TableEditorPlaceholder()
            }
        }
    }
}
