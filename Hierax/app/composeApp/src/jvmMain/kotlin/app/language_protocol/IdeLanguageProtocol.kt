//package app.language_protocol
//
//// LspClient.kt
//
//// The IdeLanguageClient class from the previous examples remains UNCHANGED.
//// This is the beauty of LSP. Your client doesn't care what language it's talking to.
//import org.eclipse.lsp4j.*
//import org.eclipse.lsp4j.jsonrpc.messages.Either
//import org.eclipse.lsp4j.launch.LSPLauncher
//import org.eclipse.lsp4j.services.LanguageClient
//import org.eclipse.lsp4j.services.LanguageServer
//import java.io.File
//import java.util.concurrent.CompletableFuture
//
///**
// * The client implementation.
// *
// * This class is responsible for *reacting* to messages sent *from* the server.
// * For example, when the server sends diagnostics (errors/warnings), the `publishDiagnostics`
// * method in this class will be called.
// */
//class IdeLanguageClient : LanguageClient {
//
//    override fun telemetryEvent(obj: Any?) {
//        println("CLIENT: Received telemetry event: $obj")
//    }
//
//    override fun publishDiagnostics(diagnosticsParams: PublishDiagnosticsParams) {
//        println("CLIENT: Received diagnostics for ${diagnosticsParams.uri}")
//        diagnosticsParams.diagnostics.forEach { d ->
//            println("  -> [${d.severity}] ${d.message} at ${d.range}")
//        }
//        // In a real IDE, you would use this to show red squiggles in the editor.
//    }
//
//    override fun showMessage(messageParams: MessageParams) {
//        println("CLIENT: Received message [${messageParams.type}] ${messageParams.message}")
//        // In a real IDE, you would show a notification popup.
//    }
//
//    override fun showMessageRequest(requestParams: ShowMessageRequestParams): CompletableFuture<MessageActionItem> {
//        println("CLIENT: Received message request: ${requestParams.message}")
//        // In a real IDE, you would show a dialog with buttons.
//        return CompletableFuture.completedFuture(null)
//    }
//
//    override fun logMessage(message: MessageParams) {
//        println("CLIENT: Received log message [${message.type}] ${message.message}")
//    }
//}
//
//fun main() {
//    // =======================================================================
//    // === 1. DEFINE THE DEDICATED GO PROJECT FOLDER ===
//    // This is the directory we want our LSP server to treat as the root.
//    // IMPORTANT: Make sure this folder exists and contains your go.mod and a .go file.
//    val projectRootPath = "C:\\Users\\moham\\Desktop\\go project"
//    val projectRootFile = File(projectRootPath)
//    // =======================================================================
//
//    // === 2. CONFIGURE AND LAUNCH THE SERVER PROCESS ===
//    val goplsPath = "C:\\Users\\moham\\go\\bin\\gopls.exe"
//    val goRoot = "C:\\Program Files\\Go" // VERIFY THIS PATH ON YOUR SYSTEM
//    val goBinPath = "C:\\Users\\moham\\go\\bin"
//
//    println("MAIN: Starting gopls language server for project: $projectRootPath")
//
//    val processBuilder = ProcessBuilder(goplsPath)
//
//    // Set the working directory for the gopls process. This is good practice.
//    processBuilder.directory(projectRootFile)
//
//    // Create a clean environment to avoid PATH issues from the IDE
//    processBuilder.environment().clear()
//
//    // Add essential Go and Windows system variables
//    val systemEnv = System.getenv()
//    processBuilder.environment()["GOROOT"] = goRoot
//    processBuilder.environment()["GOPATH"] = "C:\\Users\\moham\\go"
//    processBuilder.environment()["SystemRoot"] = systemEnv["SystemRoot"]
//    processBuilder.environment()["LOCALAPPDATA"] = systemEnv["LOCALAPPDATA"]
//    processBuilder.environment()["USERPROFILE"] = systemEnv["USERPROFILE"]
//    processBuilder.environment()["SystemDrive"] = systemEnv["SystemDrive"]
//
//    // Construct a minimal, guaranteed-correct PATH
//    val minimalPath = "$goBinPath;${goRoot}\\bin;${systemEnv["SystemRoot"]}\\System32"
//    processBuilder.environment()["PATH"] = minimalPath
//
//    // Start the process
//    val process = processBuilder.start()
//
//    // Redirect server's error stream for easier debugging
//    Thread { process.errorStream.transferTo(System.err) }.start()
//
//
//    // === 3. CONNECT THE CLIENT TO THE SERVER ===
//    val client = IdeLanguageClient()
//    val launcher = LSPLauncher.createClientLauncher(client, process.inputStream, process.outputStream)
//    val server: LanguageServer = launcher.remoteProxy
//    launcher.startListening()
//    println("MAIN: Connection established.")
//
//
//    // === 4. INITIALIZE THE SERVER using the dedicated project path ===
//    val initializeParams = InitializeParams().apply {
//        processId = ProcessHandle.current().pid().toInt()
//        // Use our dedicated project folder as the root URI
//        rootUri = projectRootFile.toLspUriString()
//        capabilities = ClientCapabilities()
//    }
//    val futureInitialize: CompletableFuture<InitializeResult> = server.initialize(initializeParams)
//    val initializeResult = futureInitialize.get()
//    println("MAIN: Server initialized.")
//    server.initialized(InitializedParams())
//    println("MAIN: Sent 'initialized' notification.")
//
//
//    // === 5. SIMULATE OPENING A FILE and requesting completions ===
//    val goFile = File(projectRootFile, "dummy.go")
//    val fileUri = goFile.toLspUriString()
//    val fileContent = """
//        package main
//
//        import "fmt"
//
//        func main() {
//            fmt.
//        }
//    """.trimIndent()
//
//    server.textDocumentService.didOpen(
//        DidOpenTextDocumentParams(TextDocumentItem(fileUri, "go", 1, fileContent))
//    )
//    println("\nMAIN: Sent 'didOpen' for $fileUri")
//
//    val completionPosition = Position(5, 8)
//    println("MAIN: Requesting code completions at $completionPosition...")
//    val completionParams = CompletionParams(TextDocumentIdentifier(fileUri), completionPosition)
//
//    val futureCompletions = server.textDocumentService.completion(completionParams)
//    val completionResult = futureCompletions.get()
//
//    // Print the results
//    println("MAIN: Received completion response!")
//    println("=========================================")
//    println("         GO CODE SUGGESTIONS")
//    println("=========================================")
//    val completionItems = completionResult.left ?: completionResult.right.items
//    if (completionItems.isEmpty()) {
//        println("No suggestions found.")
//    } else {
//        completionItems.take(15).forEach { item ->
//            println(" -> Label: ${item.label} (${item.kind})")
//            if (item.detail != null) {
//                println("      Detail: ${item.detail}")
//            }
//        }
//    }
//    println("=========================================\n")
//
//
//    // === 6. SHUTDOWN THE SERVER ===
//    println("MAIN: Shutting down server...")
//    server.shutdown().get()
//    server.exit()
//    process.waitFor()
//    println("MAIN: Done.")
//}
//
//fun File.toLspUriString(): String {
//    var uriString = this.toURI().toString()
//    if (uriString.startsWith("file:/") && !uriString.startsWith("file:///")) {
//        uriString = "file:///" + uriString.substring("file:/".length)
//    }
//    return uriString
//}
