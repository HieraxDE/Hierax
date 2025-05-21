package org.hierax.hierax

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue


@Composable
fun TextWithTab(projectPath: String) {
    var textState by remember { mutableStateOf(TextFieldValue("fun main() {\n    println(\"Hello from $projectPath\")\n}")) }
    TextField(
        value = textState,
        onValueChange = { textState = it },
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
        placeholder = { Text("sas") },
        maxLines = Int.MAX_VALUE
    )
}