package com.github.hierax

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.hierax.icons.playIcon
import com.github.hierax.widgets.FileChooser
import com.github.hierax.widgets.TextWithTab
import java.io.File

sealed class Screen {
    object Welcome : Screen()
    data class Editor(val projectPath: File) : Screen()
}

@Composable
fun App() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Welcome) }

    when (val screen = currentScreen) {
        is Screen.Welcome -> WelcomeScreen(
            recentProjects = listOf(
                "/Users/bob/Projects/sas/build.gradle.kts",
                "/Users/bob/Projects/sus/build.gradle.kts",
                "/Users/bob/Projects/ses/build.gradle.kts"
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
    onOpenProject: (File) -> Unit,
    onOpenFromFolder: (File) -> Unit
) {
    var showFileDialog by remember { mutableStateOf(false) }
    var selectedFile by remember { mutableStateOf<File?>(null) }

    MaterialTheme(colorScheme = darkColorScheme()) {
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
                            .clickable {
                                println(File(".").absolutePath)
                                onOpenProject(File("build.gradle.kts"))
                            }
                            .padding(8.dp)
                    )
                }
            }

            Column {
                Button(onClick = { showFileDialog = true }) {
                    Text("Open File")
                }

                selectedFile?.let {
                    onOpenProject(it)
                }

                FileChooser(
                    show = showFileDialog,
                    onFileSelected = { selectedFile = it },
                    onDismiss = { showFileDialog = false }
                )
            }
        }

    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(projectPath: File, onRun: () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme()) {
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