package app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.widgets.DEFAULT_CODE
import app.widgets.FileChooser
import app.widgets.Ide
import java.awt.Dimension
import java.io.File


sealed class Screen {
    object Welcome : Screen()
    data class Editor(val code: String) : Screen()
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
            onOpenProject = { code -> currentScreen = Screen.Editor(code) },
        )

        is Screen.Editor -> Ide(screen.code)
    }
}

@Composable
fun WelcomeScreen(
    recentProjects: List<String>,
    onOpenProject: (String) -> Unit,
) {
    var showFileDialog by remember { mutableStateOf(false) }
    var selectedFile by remember { mutableStateOf<String>("") }

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
                                onOpenProject(DEFAULT_CODE)
                            }
                            .padding(8.dp)
                    )
                }
            }

            Column {
                Button(onClick = { showFileDialog = true }) {
                    Text("Open File")
                }

                selectedFile.let {
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



fun main() = application {
    Window(
        title = "app",
        state = rememberWindowState(width = 800.dp, height = 600.dp),
        onCloseRequest = ::exitApplication,
    ) {
        window.minimumSize = Dimension(350, 600)
        App()
    }
}
