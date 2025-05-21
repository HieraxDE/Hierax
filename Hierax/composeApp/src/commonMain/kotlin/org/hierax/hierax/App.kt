package org.hierax.hierax

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import org.hierax.hierax.icons.playIcon

sealed class Screen {
    object Welcome : Screen()
    data class Editor(val projectPath: String) : Screen()
}

@Composable
fun App() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Welcome) }

    when (val screen = currentScreen) {
        is Screen.Welcome -> WelcomeScreen(
            recentProjects = listOf(
                "/Users/bob/Projects/sas",
                "/Users/bob/Projects/sus",
                "/Users/bob/Projects/ses"
            ),
            onOpenProject = { path -> currentScreen = Screen.Editor(path) },
            onOpenFromFolder = { path -> currentScreen = Screen.Editor(path) }
        )

        is Screen.Editor -> EditorScreen(
            projectPath = screen.projectPath,
            onRun = { println("Run project: ${screen.projectPath}") }
        )
    }
}

@Composable
fun WelcomeScreen(
    recentProjects: List<String>,
    onOpenProject: (String) -> Unit,
    onOpenFromFolder: (String) -> Unit
) {
    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Welcome!", style = MaterialTheme.typography.headlineMedium)
            Text("Recent projects:", style = MaterialTheme.typography.titleMedium)

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(recentProjects) { path ->
                    Text(
                        text = path,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenProject(path) }
                            .padding(8.dp)
                    )
                }
            }

            Button(onClick = {
                onOpenFromFolder("manual selection") // заглушка
            }) {
                Text("Open project from folder...")
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(projectPath: String, onRun: () -> Unit) {
    MaterialTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Editing: $projectPath") },
                actions = {
                    IconButton(onClick = onRun) {
                        Icon(playIcon, contentDescription = "Run")
                    }
                }
            )
            TextWithTab(projectPath)
        }
    }
}