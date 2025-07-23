package app.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.KotlinSyntaxHighlighter
import kotlin.math.roundToInt

@Composable
fun IDECodeEditor() {
    var showPopup by remember { mutableStateOf(true) }
    var popupPosition by remember { mutableStateOf(IntOffset(0, 0)) }
    var textFieldPosition by remember { mutableStateOf(IntOffset(0, 0)) }

    var textLayoutResult: MutableState<TextLayoutResult?> = remember { mutableStateOf(null) }

    val onTextLayout: (TextLayoutResult) -> Unit = {
        textLayoutResult.value = it
    }

    var textState by remember {
        mutableStateOf(
            TextFieldValue(
                DEFAULT_CODE,
                selection = TextRange(0)
            )
        )
    }

    val highlights = remember(textState.text) {
        KotlinSyntaxHighlighter().highlight(textState.text).map { (range, color) ->
            Pair(range, color.color)
        }
    }

    LaunchedEffect(textState.selection, textFieldPosition, textLayoutResult) {
        textLayoutResult.value?.let { layoutResult ->
            val cursorIndex = textState.selection.start
            val cursorRect = layoutResult.getCursorRect(cursorIndex.coerceIn(0, textState.text.length))

            val absoluteX = textFieldPosition.x + cursorRect.left.roundToInt()
            val absoluteY = textFieldPosition.y + cursorRect.bottom.roundToInt()

            popupPosition = IntOffset(absoluteX, absoluteY)
        }
    }

    if (showPopup) {
        Popup(
            alignment = Alignment.TopStart,
            offset = popupPosition,
            onDismissRequest = { showPopup = false }
        ) {
            Surface(
                elevation = 8.dp,
                color = MaterialTheme.colors.surface,
                modifier = Modifier.background(Color.White)
            ) {
                Text(
                    text = "suggestions go here",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
        CodeEditor(
            textFieldValue = textState,
            onValueChange = { textState = it },
            modifier = Modifier.fillMaxSize().padding(16.dp),
            codeEditorModifier = Modifier.fillMaxSize().onGloballyPositioned { layoutCoordinates ->
                val position = layoutCoordinates.positionInWindow()
                textFieldPosition = IntOffset(position.x.roundToInt(), position.y.roundToInt())
            }.onPreviewKeyEvent { keyEvent: KeyEvent ->
                println(keyEvent.key)
                if (
                    keyEvent.isCtrlPressed &&
                    keyEvent.awtEventOrNull?.keyChar == ' '
                ) {
                    showPopup = true
                    return@onPreviewKeyEvent true
                } else if (showPopup && keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Spacebar) {
                    showPopup = false
//                } else if (keyEvent.type != KeyEventType.KeyDown && keyEvent.awtEventOrNull?.keyChar == ' ') {
//                    return@onPreviewKeyEvent true
                }
                false
            },
            textLayoutResultState = textLayoutResult,
            highlightColor = Color.Yellow.copy(alpha = 0.2f),
            editorTextStyle = LocalTextStyle.current.copy(
                color = MaterialTheme.colors.onSurface,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            ),
            highlightRanges = highlights.map {
                Pair(it.first, it.second)
            }
        )
    }
}

fun main() = application {
    Window(
        title = "Code Editor",
        state = rememberWindowState(),
        onCloseRequest = ::exitApplication
    ) {
        IDECodeEditor()
    }
}
