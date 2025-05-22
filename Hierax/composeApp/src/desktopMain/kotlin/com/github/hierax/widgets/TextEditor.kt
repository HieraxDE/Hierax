package com.github.hierax.widgets


import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File


@Composable
fun TextWithTab(projectPath: File) {
    var textState by remember { mutableStateOf(TextFieldValue("")) }

    LaunchedEffect(projectPath) {
        val text = withContext(Dispatchers.IO) {
            projectPath.readText()
        }
        textState = TextFieldValue(text)
    }
    TextField(
        value = textState,
        onValueChange = { textState = it },
        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = LocalContentColor.current),
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Tab) {
                    val text = textState.text
                    val start = textState.selection.start
                    val end = textState.selection.end
                    val newText = buildString {
                        append(text.substring(0, start))
                        append("    ")
                        append(text.substring(end))
                    }
                    val newCursor = start + 4
                    textState = TextFieldValue(
                        text = newText,
                        selection = TextRange(newCursor)
                    )
                    true
                } else {
                    false
                }
            },
        maxLines = Int.MAX_VALUE
    )
}