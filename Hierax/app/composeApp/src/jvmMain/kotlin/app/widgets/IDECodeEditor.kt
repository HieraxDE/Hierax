package app.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.KotlinSyntaxHighlighter
import app.data.Suggestion
import app.language_protocol.LspClientManager
import app.language_protocol.toLspUriString
import java.io.File
import kotlin.math.roundToInt

@Composable
fun IDECodeEditor(
    file: String = "",
    lspClientManager: LspClientManager,
    suggestions: List<Suggestion>,
    isPopupVisible: MutableState<Boolean>
) {
    var showPopup by remember { mutableStateOf(true) }
    var popupPosition by remember { mutableStateOf(IntOffset(0, 0)) }
    var textFieldLayoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    var textLayoutResult: MutableState<TextLayoutResult?> = remember { mutableStateOf(null) }

    val onTextLayout: (TextLayoutResult) -> Unit = {
        textLayoutResult.value = it
    }

    val editorStates = remember { mutableStateMapOf<String, TextFieldValue>() }

    var selectedIndex by remember { mutableStateOf(-1) }

    val onSelect: (Suggestion) -> Unit = { suggestion ->

    }

    println("recomposed")

    LaunchedEffect(file) {
        println("relaunched")
        if (file !in editorStates) {
            editorStates[file] = TextFieldValue(File(file).readLines().joinToString("\n") {
                it
            })
        }
    }

    var textState = editorStates[file] ?: TextFieldValue("")

        val highlights = remember(textState.text) {
            KotlinSyntaxHighlighter().highlight(textState.text).map { (range, color) ->
                Pair(range, color.color)
            }
        }

        LaunchedEffect(textState.selection, textFieldLayoutCoordinates, textLayoutResult) {
                val layoutResult = textLayoutResult.value ?: return@LaunchedEffect
                val coordinates = textFieldLayoutCoordinates ?: return@LaunchedEffect

                val cursorIndex = textState.selection.start
                val cursorRect = layoutResult.getCursorRect(cursorIndex.coerceIn(0, textState.text.length))

            if (cursorIndex > textState.text.length) return@LaunchedEffect

                val cursorScreenPosition = Offset(cursorRect.left, cursorRect.bottom)

                popupPosition = IntOffset(cursorScreenPosition.x.roundToInt() + 37, cursorScreenPosition.y.roundToInt() + cursorRect.height.roundToInt() + 37)
        }

    if (isPopupVisible.value) {
        val density = LocalDensity.current

        SuggestionPopup(
            suggestions = suggestions,
            onSuggestionClicked = { suggestion ->
                textState = textState.copy(suggestion.name)
                isPopupVisible.value = false
            },
            onDismissRequest = {
                isPopupVisible.value = false
            },
            offset = popupPosition,
            selectedIndex
        )
    }

        Box(modifier = Modifier.fillMaxSize()) {
            CodeEditor(
                textFieldValue = textState,
                onValueChange = {
                    editorStates[file] = it
                    println(it.text)
                    lspClientManager.didChangeFile(File(file).toLspUriString(), it.text)
                },
                modifier = Modifier.fillMaxSize().padding(16.dp).background(Color.White),
                codeEditorModifier = Modifier.fillMaxSize().onGloballyPositioned { layoutCoordinates ->
                    val position = layoutCoordinates.positionInWindow()
                    textFieldLayoutCoordinates = layoutCoordinates
                }.onPreviewKeyEvent { keyEvent: KeyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        if (isPopupVisible.value) {
                            when (keyEvent.key) {
                                Key.DirectionDown -> {
                                    selectedIndex = (selectedIndex + 1) % suggestions.size
                                    return@onPreviewKeyEvent true
                                }

                                Key.DirectionUp -> {
                                    selectedIndex = (selectedIndex - 1 + suggestions.size) % suggestions.size
                                    return@onPreviewKeyEvent true
                                }

                                Key.Enter -> {
                                    if (selectedIndex != -1) {
                                        (suggestions[selectedIndex])
                                        val text = editorStates[file]!!
                                        editorStates[file] = editorStates[file]!!.copy(text.text.take(text.selection.start) + suggestions[selectedIndex].name + text.text.drop(text.selection.start), TextRange(text.selection.start + suggestions[selectedIndex].name.length))
                                        isPopupVisible.value = false
                                    }
                                    return@onPreviewKeyEvent true
                                }
                            }
                        }
                    }
                    println(keyEvent.key)
                    if (
                        keyEvent.isCtrlPressed &&
                        keyEvent.awtEventOrNull?.keyChar == ' '
                    ) {
                        isPopupVisible.value = true
                        val (cursorLine, cursorOffset) = getLineAndCharacter(textState.text, textState.selection.start)
                        lspClientManager.requestCompletions(File(file).toLspUriString(), cursorLine, cursorOffset)
                        return@onPreviewKeyEvent true
                    } else if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Spacebar) {
                        isPopupVisible.value = false
//                } else if (keyEvent.type != KeyEventType.KeyDown && keyEvent.awtEventOrNull?.keyChar == ' ') {
//                    return@onPreviewKeyEvent true
                    } else if (keyEvent.type == KeyEventType.KeyDown) {
                        val (cursorLine, cursorOffset) = getLineAndCharacter(textState.text, textState.selection.start)
                        lspClientManager.requestCompletions(File(file).toLspUriString(), cursorLine, cursorOffset)
                        isPopupVisible.value = true
                    }
                    false
                },
                textLayoutResultState = textLayoutResult,
                highlightColor = Color.Yellow.copy(alpha = 0.2f),
                editorTextStyle = LocalTextStyle.current.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineBreak = LineBreak.Simple
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
//        IDECodeEditor()
    }
}

/**
 * Converts a string offset (cursor position) into a line and character number.
 *
 * @param text The full text content.
 * @param offset The integer offset of the cursor (e.g., from TextFieldValue.selection.start).
 * @return A Pair where `first` is the line number (0-indexed) and `second` is the character number (0-indexed).
 */
fun getLineAndCharacter(text: String, offset: Int): Pair<Int, Int> {
    val validOffset = offset.coerceIn(0, text.length)

    var line = 0
    var lineStartOffset = 0

    for (i in 0 until validOffset) {
        if (text[i] == '\n') {
            line++
            lineStartOffset = i + 1
        }
    }

    val character = validOffset - lineStartOffset
    return Pair(line, character)
}
