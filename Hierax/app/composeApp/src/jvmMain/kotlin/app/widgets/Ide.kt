package app.widgets

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.configurations.Environment
import app.configurations.KotlinCompilerBinaryPath
import app.configurations.ProgrammingLanguage
import app.data.Suggestion
import app.language_protocol.LspClientManager
import app.language_protocol.createLanguageClientForLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.io.File
import kotlin.io.path.createTempDirectory

interface CodeExecutor {
    /**
     * Executes the code in the given file.
     * @return A Flow that emits lines of output from the execution process.
     */
    fun execute(file: File, environment: Environment): Flow<String>
}

class KotlinExecutor : CodeExecutor {
    override fun execute(file: File, environment: Environment): Flow<String> = flow {
        val tempDir = createTempDirectory("kotlin-exec-").toFile()
        try {
            emit("Compiling ${file.name}...")
            val compileProcess = ProcessBuilder(environment.compilerPath, file.absolutePath, "-d", tempDir.absolutePath)
                .redirectErrorStream(true)
                .start()

            compileProcess.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { emit(it) }
            }

            val compileExitCode = compileProcess.waitFor()
            if (compileExitCode != 0) {
                emit("--- Compilation failed with exit code $compileExitCode ---")
                return@flow
            }
            emit("Compilation successful.")

            val mainClassName = file.nameWithoutExtension.replaceFirstChar { it.uppercase() } + "Kt"
            emit("\nExecuting $mainClassName...")
            emit("--- Output ---")

            val kotlinProcess = ProcessBuilder("kotlin", "-cp", tempDir.absolutePath, mainClassName)
                .redirectErrorStream(true)
                .start()

            kotlinProcess.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { emit(it) }
            }

            val runExitCode = kotlinProcess.waitFor()
            emit("Process finished with exit code: $runExitCode")

        } catch (e: Exception) {
            emit("An error occurred during execution: ${e.message}")
        } finally {
            tempDir.deleteRecursively()
        }
    }.flowOn(Dispatchers.IO)
}

fun createCodeExecutor(environment: Environment): CodeExecutor? {
    return when (environment.programmingLanguage) {
        ProgrammingLanguage.Kotlin -> KotlinExecutor()
        else -> null
    }
}


@Composable
fun Ide() {
    val openFiles = remember { mutableStateListOf<File>() }
    var selectedTabIndex by remember { mutableStateOf(-1) }

    val environment = Environment()

    val scope = rememberCoroutineScope()

    val lspClientManager = remember { createLanguageClientForLanguage(environment.programmingLanguage, scope, environment.projectRootPath) }

    var isPopupVisible = remember { mutableStateOf(false) }

    val isUserTyped by remember { mutableStateOf(true) }

    var suggestions by remember {
        mutableStateOf(listOf<Suggestion>())
    }

    LaunchedEffect(suggestions) {
        if (suggestions.isNotEmpty()) {
            isPopupVisible.value = true
        } else {
            isPopupVisible.value = false
        }
    }

    val onFileSelected: (File) -> Unit = { file ->
        val existingIndex = openFiles.indexOf(file)
        if (existingIndex != -1) {
            selectedTabIndex = existingIndex
        } else {
            openFiles.add(file)
            CoroutineScope(Dispatchers.IO).launch {
                selectedTabIndex = openFiles.lastIndex
            }
        }

        lspClientManager.didOpenFile(file, file.readLines().joinToString("\n") {
            it
        })
    }

    val onTabClosed: (File) -> Unit = { fileToClose ->
        val indexToClose = openFiles.indexOf(fileToClose)
        if (indexToClose != -1) {
            val newSelectedIndex = when {
                openFiles.size == 1 -> -1
                indexToClose == selectedTabIndex -> (indexToClose - 1).coerceAtLeast(0)
                indexToClose < selectedTabIndex -> selectedTabIndex - 1
                else -> selectedTabIndex
            }
            openFiles.removeAt(indexToClose)
            selectedTabIndex = newSelectedIndex
        }
    }

    var consoleOutput by remember { mutableStateOf("Welcome! Open a Kotlin file and click 'Execute'.\n") }
    val codeExecutor: CodeExecutor = remember { KotlinExecutor() }

    LaunchedEffect(lspClientManager) {
            launch {
                lspClientManager.connectionStatus.collect { status ->
                    println("[UI]: connection status changed to: $status")
                }
            }
            launch {
                lspClientManager.diagnostics.collect { diagnosticsMap ->
                    // this will be called whenever diagnostics for ANY file are updated
                    println("[UI]: diagnosis updated:")
                    diagnosticsMap.forEach { (uri, diagnostics) ->
                        println("  File: $uri")
                        diagnostics.forEach { d ->
                            println("    -> [${d.severity}] ${d.message}")
                        }
                    }
                }
            }
            launch {
                lspClientManager.completionSuggestions.collect { languageServerSuggestions ->
                    // this will be called ONLY when new suggestions are available
//                    if (languageServerSuggestions.isNotEmpty()) {
                        println("\n[UI]: ===== RECEIVED SUGGESTIONS =====")
                        languageServerSuggestions.take(10).forEach {
                            println("  -> ${it.label} (${it.kind})")
                        }
                        println("====================================\n")

                        suggestions = languageServerSuggestions.mapIndexed { index, item ->
                            Suggestion(index.toString(), item.label, Icons.Default.AddBox)
                        }
//                    }
                }
            }
    }

    DisposableEffect(lspClientManager) {
//        scope.launch {
            lspClientManager.start()
//        }

        onDispose {
            lspClientManager.shutdown()
        }
    }

    Column {
        Row {
            Spacer(Modifier.width(30.dp))
            Text(
                text = "Ide",
                fontSize = 17.sp
            )
        }

        HorizontalDivider()

        MultiSplitPaneLayout(
            Modifier,
            Orientation.Vertical,
            0.dp,
            listOf(
                {
                    MultiSplitPaneLayout(
                        Modifier,
                        Orientation.Horizontal,
                        0.dp,
                        listOf({
                            FileExplorer(
                                rootPath = File(System.getProperty("user.home")).path,
                                onFileSelected = onFileSelected,
                                modifier = Modifier.fillMaxHeight().width(250.dp)
                            )
                        }, {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = {
                                            val currentFile = openFiles.getOrNull(selectedTabIndex)
                                            if (currentFile != null) {
                                                consoleOutput = ""
                                                scope.launch {
                                                    codeExecutor.execute(currentFile, environment).collect { line ->
                                                        consoleOutput += line + "\n"
                                                    }
                                                }
                                            }
                                        },
                                        enabled = selectedTabIndex != -1
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = "Execute")
                                        Spacer(Modifier.width(8.dp))
                                        Text("Execute")
                                    }
                                }
                                HorizontalDivider()

                                CodeEditorTabLayout(
                                    files = openFiles,
                                    selectedTabIndex = selectedTabIndex,
                                    onTabSelected = { index -> selectedTabIndex = index },
                                    onTabClosed = onTabClosed,
                                    lspClientManager,
                                    suggestions,
                                    isPopupVisible
                                )
                            }
                        })
                    )
                }, {
                    val scrollState = rememberScrollState()
                    LaunchedEffect(consoleOutput) {
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }

                    BasicTextField(
                        value = consoleOutput,
                        onValueChange = {  },
                        readOnly = true,
                        modifier = Modifier.fillMaxSize().padding(8.dp).verticalScroll(scrollState)
                    )
                }
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditorTabLayout(
    files: List<File>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    onTabClosed: (File) -> Unit,
    lspClientManager: LspClientManager,
    suggestions: List<Suggestion>,
    isPopupVisible: MutableState<Boolean>
) {
    if (files.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No files are open. Select a file from the explorer.")
        }
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        PrimaryScrollableTabRow(selectedTabIndex = selectedTabIndex, edgePadding = 0.dp) {
            files.forEachIndexed { index, file ->
                Tab(
                    selected = (selectedTabIndex == index),
                    onClick = { onTabSelected(index) },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = file.name, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.width(4.dp))
                            IconButton(
                                onClick = { onTabClosed(file) },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close tab",
                                    tint = LocalContentColor.current
                                )
                            }
                        }
                    }
                )
            }
        }

        if (selectedTabIndex in files.indices) {
            val selectedFile = files[selectedTabIndex]
            IDECodeEditor(selectedFile.path, lspClientManager, suggestions, isPopupVisible)
        }
    }
}

fun main() = application {
    Window(
        title = "Kotlin IDE",
        state = rememberWindowState(width = 1200.dp, height = 800.dp),
        onCloseRequest = ::exitApplication
    ) {
        MaterialTheme {
            Ide()
        }
    }
}
