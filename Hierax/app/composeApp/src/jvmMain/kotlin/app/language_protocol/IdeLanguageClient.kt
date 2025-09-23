package app.language_protocol

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageServer
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.future.await

/**
 * Represents the connection status to the language server.
 * The UI can observe this to show loading indicators or error messages.
 */
enum class LspConnectionStatus {
    DISCONNECTED,
    INITIALIZING,
    CONNECTED,
    ERROR
}

/**
 * A simple data class to hold information about an open document.
 */
data class Document(
    val uri: String,
    val content: String,
    val version: AtomicInteger = AtomicInteger(1)
)

/**
 * The client implementation that reacts to messages from the server.
 * It's now designed to update StateFlows instead of just printing to the console.
 *
 * @param diagnosticsStateFlow The StateFlow to update when new diagnostics are published.
 */
class IdeLanguageClient(
    private val coroutineScope: CoroutineScope,
    private val diagnosticsStateFlow: MutableStateFlow<Map<String, List<Diagnostic>>>
) : LanguageClient {

    override fun telemetryEvent(obj: Any?) {
        println("CLIENT: Received telemetry event: $obj")
    }

    /**
     * The server has sent diagnostics (errors/warnings).
     * This method updates the shared diagnostics state.
     */
    override fun publishDiagnostics(diagnosticsParams: PublishDiagnosticsParams) {
        println("CLIENT: Received diagnostics for ${diagnosticsParams.uri}")
        coroutineScope.launch {
            diagnosticsStateFlow.update { currentMap ->
                currentMap.toMutableMap().apply {
                    this[diagnosticsParams.uri] = diagnosticsParams.diagnostics
                }
            }
        }
    }

    override fun showMessage(messageParams: MessageParams) {
        println("CLIENT: Received message [${messageParams.type}] ${messageParams.message}")
    }

    override fun showMessageRequest(requestParams: ShowMessageRequestParams): CompletableFuture<MessageActionItem> {
        println("CLIENT: Received message request: ${requestParams.message}")
        return CompletableFuture.completedFuture(null)
    }

    override fun logMessage(message: MessageParams) {
        println("CLIENT: Received log message [${message.type}] ${message.message}")
    }
}

/**
 * Manages the lifecycle and communication with a Language Server.
 * This class is designed to be used as a singleton or a central service
 * within your application, providing state and actions to the UI layer.
 *
 * @param scope The CoroutineScope to run background tasks in.
 * @param serverCommand The command to execute to start the language server (e.g., "gopls").
 * @param projectRoot The root directory of the user's project.
 */
class LspClientManager(
    private val scope: CoroutineScope,
    private val serverCommand: String,
    private val projectRoot: File
) {
    private val _connectionStatus = MutableStateFlow(LspConnectionStatus.DISCONNECTED)
    val connectionStatus = _connectionStatus.asStateFlow()

    private val _diagnostics = MutableStateFlow<Map<String, List<Diagnostic>>>(emptyMap())
    val diagnostics = _diagnostics.asStateFlow()

    private val _completionSuggestions = MutableStateFlow<List<CompletionItem>>(emptyList())
    val completionSuggestions = _completionSuggestions.asStateFlow()

    private var lspProcess: Process? = null
    private var server: LanguageServer? = null
    private val openDocuments = mutableMapOf<String, Document>()

    /**
     * Starts the language server process, establishes a connection, and initializes it.
     * This is a suspend function and should be called from a coroutine.
     */
    fun start() {
        if (_connectionStatus.value != LspConnectionStatus.DISCONNECTED) {
            println("MANAGER: Server is already starting or connected.")
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                _connectionStatus.value = LspConnectionStatus.INITIALIZING
                println("MANAGER: Starting language server for project: ${projectRoot.absolutePath}")

                val processBuilder = ProcessBuilder(serverCommand)
                processBuilder.directory(projectRoot)
                val process = processBuilder.start()
                lspProcess = process

                val client = IdeLanguageClient(scope, _diagnostics)
                val launcher = LSPLauncher.createClientLauncher(
                    client,
                    process.inputStream,
                    process.outputStream
                )
                val remoteProxy = launcher.remoteProxy
                server = remoteProxy
                launcher.startListening()
                println("MANAGER: Connection established.")

                val initializeParams = InitializeParams().apply {
                    processId = ProcessHandle.current().pid().toInt()
                    rootUri = projectRoot.toLspUriString()
                    capabilities = ClientCapabilities()
                }

                val initializeResult = remoteProxy.initialize(initializeParams).await()
                println("MANAGER: Server capabilities: ${initializeResult.capabilities}")

                remoteProxy.initialized(InitializedParams())
                println("MANAGER: Server initialized.")

                _connectionStatus.value = LspConnectionStatus.CONNECTED

            } catch (e: Exception) {
                e.printStackTrace()
                _connectionStatus.value = LspConnectionStatus.ERROR
                shutdownInternal()
            }
        }
    }

    /**
     * Notifies the server that a file has been opened.
     *
     * @param file The file that was opened.
     * @param content The initial content of the file.
     */
    fun didOpenFile(file: File, content: String) {
        val server = this.server ?: return
        val uri = file.toLspUriString()
        if (openDocuments.containsKey(uri)) return

        val document = Document(uri, content)
        openDocuments[uri] = document

        val params = DidOpenTextDocumentParams(
            TextDocumentItem(uri, "go", document.version.get(), content)
        )
        server.textDocumentService.didOpen(params)
        println("MANAGER: Sent 'didOpen' for $uri")
    }

    /**
     * Notifies the server that a file's content has changed.
     *
     * @param uri The URI of the file that changed.
     * @param newContent The complete new content of the file.
     */
    fun didChangeFile(uri: String, newContent: String) {
        val server = this.server ?: return
        val document = openDocuments[uri] ?: return

        val newVersion = document.version.incrementAndGet()
        document.copy(content = newContent)

        val params = DidChangeTextDocumentParams(
            VersionedTextDocumentIdentifier(uri, newVersion),
            listOf(TextDocumentContentChangeEvent(newContent))
        )
        server.textDocumentService.didChange(params)
        println("MANAGER: Sent 'didChange' for $uri (version $newVersion)")
    }


    /**
     * Asynchronously requests code completions from the server for a specific position.
     * The results will be published to the `completionSuggestions` StateFlow.
     *
     * @param uri The URI of the file to get completions for.
     * @param line The line number (0-indexed).
     * @param character The character offset in the line (0-indexed).
     */
    fun requestCompletions(uri: String, line: Int, character: Int) {
        val server = this.server ?: return
        scope.launch(Dispatchers.IO) {
            try {
                println("MANAGER: Requesting completions for $uri at ($line, $character)")
                val completionParams = CompletionParams(TextDocumentIdentifier(uri), Position(line, character))

                _completionSuggestions.value = emptyList()

                val result: Either<List<CompletionItem>, CompletionList> = server.textDocumentService.completion(completionParams).await()
                val items = result.let { it.left ?: it.right?.items ?: emptyList() }

                _completionSuggestions.value = items
                println("MANAGER: Received ${items.size} completion suggestions.")

            } catch (e: Exception) {
                e.printStackTrace()
                _completionSuggestions.value = emptyList()
            }
        }
    }

    private fun shutdownInternal() {
        server?.let {
            try {
                it.shutdown().get()
                it.exit()
            } catch (e: Exception) {
                println("MANAGER: Error during server shutdown: ${e.message}")
            }
        }
        lspProcess?.destroyForcibly()
        lspProcess = null
        server = null
        _connectionStatus.value = LspConnectionStatus.DISCONNECTED
        openDocuments.clear()
        _diagnostics.value = emptyMap()
        _completionSuggestions.value = emptyList()
        println("MANAGER: Server shutdown complete.")
    }

    /**
     * Shuts down the language server gracefully.
     * This is the public-facing version that launches a coroutine.
     */
    fun shutdown() {
        scope.launch(Dispatchers.IO) {
            shutdownInternal()
        }
    }
}

fun File.toLspUriString(): String {
    return toURI().toString().replace("file:/", "file:///")
}

fun main() = runBlocking {
    val projectRoot = File("C:\\Users\\moham\\Desktop\\go project")
    val goplsPath = "C:\\Users\\moham\\go\\bin\\gopls.exe"

    val appScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val lspManager = LspClientManager(appScope, goplsPath, projectRoot)

    val uiJob = appScope.launch {
        launch {
            lspManager.connectionStatus.collect { status ->
                println("[UI]: Connection status changed to: $status")
            }
        }
        launch {
            lspManager.diagnostics.collect { diagnosticsMap ->
                println("[UI]: Diagnostics updated:")
                diagnosticsMap.forEach { (uri, diagnostics) ->
                    println("  File: $uri")
                    diagnostics.forEach { d ->
                        println("    -> [${d.severity}] ${d.message}")
                    }
                }
            }
        }
        launch {
            lspManager.completionSuggestions.collect { suggestions ->
                if (suggestions.isNotEmpty()) {
                    println("\n[UI]: ===== RECEIVED SUGGESTIONS =====")
                    suggestions.take(10).forEach {
                        println("  -> ${it.label} (${it.kind})")
                    }
                    println("====================================\n")
                }
            }
        }
    }

    lspManager.start()
    delay(2000)

    val goFile = File(projectRoot, "dummy.go")
    val fileContent = """
        package main

        import "fmt"

        func main() {
            fmt.
        }
    """.trimIndent()
    lspManager.didOpenFile(goFile, fileContent)

    delay(2000)

    val fileUri = goFile.toLspUriString()
    val cursorLine = 5
    val cursorChar = 8
    lspManager.requestCompletions(uri = fileUri, line = cursorLine, character = cursorChar)

    delay(5000)

    println("MAIN: Shutting down...")
    lspManager.shutdown()
    appScope.cancel()
    println("MAIN: Done.")
}
