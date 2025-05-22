package com.github.hierax.widgets

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import dialogs.file.FileChooserDialog
import java.io.File

@Composable
fun FileChooser(
    show: Boolean,
    onFileSelected: (File) -> Unit,
    onDismiss: () -> Unit
) {
    if (show) {
        FileChooserDialog(
            title = "Select a File",
            allowedExtensions = listOf("txt", "pdf", "md"),
            folderIconColor = MaterialTheme.colorScheme.tertiary,
            fileIconColor = MaterialTheme.colorScheme.primary,
            onFileSelected = {
                onFileSelected(it)
                onDismiss()
            },
            onCancel = onDismiss
        )
    }
}