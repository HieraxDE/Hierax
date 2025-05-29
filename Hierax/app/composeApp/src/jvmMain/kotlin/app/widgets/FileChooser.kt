package app.widgets

import androidx.compose.runtime.*
import androidx.compose.material3.*
import dialogs.file.FileChooserDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun FileChooser(
    show: Boolean,
    onFileSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    if (show) {
        FileChooserDialog(
            title = "Select a File",
            allowedExtensions = listOf("txt", "md", "kt", "java"),
            folderIconColor = MaterialTheme.colorScheme.tertiary,
            fileIconColor = MaterialTheme.colorScheme.primary,
            onFileSelected = { file ->
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val content = File(file.path).readText()
                        onFileSelected(content)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        onFileSelected("Error reading file: ${e.message}")
                    } finally {
                        onDismiss()
                    }
                }
            },
            onCancel = onDismiss
        )
    }
}