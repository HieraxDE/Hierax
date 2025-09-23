package app.widgets

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.compose.ui.tooling.preview.Preview
import java.io.File

/**
 * A composable that displays a file system tree.
 *
 * @param rootPath The starting path to display. If null, it will automatically select
 *                 the default root for the current operating system.
 * @param onFileSelected A lambda that is invoked when a file or folder is **double-clicked**.
 *                       The selected item is passed as a `File` object.
 * @param modifier A `Modifier` for this composable.
 */
@Composable
fun FileExplorer(
    rootPath: String? = null,
    onFileSelected: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val actualRootPath = remember(rootPath) {
        rootPath ?: getDefaultRootPath()
    }
    val rootFile = remember(actualRootPath) { File(actualRootPath) }

    // This state now tracks the "focused" item (from a single click)
    var focusedFile by remember { mutableStateOf<File?>(null) }
    var expandedFolders by remember { mutableStateOf(setOf(rootFile)) }

    val files = remember(rootFile, expandedFolders) {
        rootFile.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
    }

    LazyColumn(modifier = modifier) {
        items(files, key = { it.absolutePath }) { file ->
            FileNode(
                file = file,
                level = 0,
                focusedFile = focusedFile,
                expandedFolders = expandedFolders,
                onFileFocus = { focusedFile = it }, // Single-click updates focus
                onFileDoubleClick = { onFileSelected(it) }, // Double-click calls the user's lambda
                onFolderToggle = { folder ->
                    expandedFolders = if (expandedFolders.contains(folder)) {
                        expandedFolders - folder
                    } else {
                        expandedFolders + folder
                    }
                }
            )
        }
    }
}

/**
 * A recursive composable that represents a single node (file or folder) in the file tree.
 */
@OptIn(ExperimentalFoundationApi::class) // Needed for combinedClickable
@Composable
private fun FileNode(
    file: File,
    level: Int,
    focusedFile: File?,
    expandedFolders: Set<File>,
    onFileFocus: (File) -> Unit,
    onFileDoubleClick: (File) -> Unit,
    onFolderToggle: (File) -> Unit
) {
    val isFocused = file == focusedFile
    val isExpanded = expandedFolders.contains(file)

    // The main clickable row for the file/folder
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.material.MaterialTheme.shapes.small)
            .background(if (isFocused) Color.LightGray.copy(alpha = 0.4f) else Color.Transparent)
            // *** KEY CHANGE: Use combinedClickable ***
            .combinedClickable(
                onClick = {
                    onFileFocus(file)
                },
                onDoubleClick = {
                    if (file.isDirectory) {
                        // Double-clicking a folder both toggles and "selects" it
                        onFolderToggle(file)

                    }
                    onFileDoubleClick(file)
                    onFileFocus(file)
                }
            )
            .padding(start = (level * 24).dp, top = 4.dp, bottom = 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // *** KEY CHANGE: The arrow icon now has its own click handler ***
        // This allows expanding/collapsing without changing focus.
        val arrowIcon = if (isExpanded) Icons.Default.ArrowDropDown else Icons.Default.ArrowRight
        if (file.isDirectory) {
            Icon(
                imageVector = arrowIcon,
                contentDescription = "Expand/Collapse Folder",
                modifier = Modifier.clickable {
                    onFolderToggle(file)
                    onFileFocus(file)
                }
            )
        } else {
            Spacer(modifier = Modifier.width(24.dp))
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Icon for file type
        Icon(
            imageVector = getIconForFile(file),
            contentDescription = "File Type",
            tint = if (file.isDirectory) Color(0xFFFBC02D) else Color.DarkGray
        )

        Spacer(modifier = Modifier.width(8.dp))

        // File name
        Text(
            text = file.name,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }

    // This part remains the same: recursively show children if expanded
    if (isExpanded && file.isDirectory) {
        val children = remember(file) {
            file.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
        }

        if (children.isEmpty() && level >= 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = ((level + 1) * 24).dp + 32.dp, top = 2.dp, bottom = 2.dp)
            ) {
                Text(
                    text = "(empty)",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        } else {
            children.forEach { child ->
                FileNode(
                    file = child,
                    level = level + 1,
                    focusedFile = focusedFile,
                    expandedFolders = expandedFolders,
                    onFileFocus = onFileFocus,
                    onFileDoubleClick = onFileDoubleClick,
                    onFolderToggle = onFolderToggle
                )
            }
        }
    }
}

/**
 * Returns an appropriate icon for a given file.
 */
@Composable
private fun getIconForFile(file: File): ImageVector {
    return when {
        file.isDirectory -> Icons.Default.Folder
        file.isTextFile() -> Icons.Default.Description // A good icon for text
        else -> Icons.Default.InsertDriveFile
    }
}

/**
 * An extension function to check if a file is likely a text file based on its extension.
 */
private fun File.isTextFile(): Boolean {
    return extension.lowercase() in listOf(
        "txt", "md", "json", "xml", "html", "css", "js", "kt", "java", "py", "c", "cpp", "h", "hpp", "gradle"
    )
}

/**
 * Determines the default root path based on the operating system.
 */
private fun getDefaultRootPath(): String {
    val os = System.getProperty("os.name").lowercase()
    return when {
        os.contains("win") -> "C:\\"
        os.contains("nix") || os.contains("nux") || os.contains("aix") -> "/"
        os.contains("mac") -> "/"
        // A sensible fallback
        else -> System.getProperty("user.home")
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Compose File Explorer") {
        MaterialTheme {
            Viewer()
        }
    }
}

@Composable
@Preview
fun Viewer() {
    var selectedFilePath by remember { mutableStateOf<String?>("No file selected") }

    Row(modifier = Modifier.fillMaxSize()) {
        // The File Explorer
        Box(modifier = Modifier.weight(1f).padding(8.dp)) {
            FileExplorer(
                // You can specify a path like "C:/Users/YourUser" or leave it null for auto-detection
                // rootPath = System.getProperty("user.home"),
                onFileSelected = { file: File ->
                    selectedFilePath = file.absolutePath
                },
                modifier = Modifier
                    .fillMaxHeight()
                    .border(1.dp, Color.Gray, MaterialTheme.shapes.medium)
            )
        }

        // A panel to show the selected file's path
        Surface(
            modifier = Modifier.weight(1f).padding(8.dp).fillMaxHeight(),
            color = MaterialTheme.colors.surface,
            elevation = 2.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Selected File:", style = MaterialTheme.typography.h6)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(selectedFilePath ?: "None")
                }
            }
        }
    }
}
