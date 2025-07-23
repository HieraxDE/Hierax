package app.widgets

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.rememberTextFieldScrollState
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.application
import app.KotlinSyntaxHighlighter
//import generateHighlights
import kotlinx.coroutines.flow.distinctUntilChanged
import java.io.File

@Composable
fun LineNumberColumn(
    textLayoutResultState: State<TextLayoutResult?>,
    lineNumberScrollState: ScrollState,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    lineNumberColor: Color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
    lineNumberBackgroundColor: Color = MaterialTheme.colors.surface.copy(alpha = 0.2f)
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val currentTextLayoutResult = textLayoutResultState.value

    val numbersColumnWidth = remember(currentTextLayoutResult, textStyle, density, textMeasurer) {
        val lines = currentTextLayoutResult?.lineCount ?: 1
        val maxLineNumText = lines.toString()
        val measuredTextWidth = textMeasurer.measure(AnnotatedString(maxLineNumText), style = textStyle).size.width
        density.run { measuredTextWidth.toDp() } + 16.dp
    }

    val lineNumbersString = remember(currentTextLayoutResult) {
        if (currentTextLayoutResult != null && currentTextLayoutResult.lineCount > 0) {
            (1..currentTextLayoutResult.lineCount).joinToString("\n")
        } else {
            "1"
        }
    }

    Box(
        modifier = modifier
            .width(numbersColumnWidth)
            .fillMaxHeight()
            .background(lineNumberBackgroundColor)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Text(
            text = lineNumbersString,
            style = textStyle,
            color = lineNumberColor,
            textAlign = TextAlign.End,
            modifier = Modifier
                .verticalScroll(lineNumberScrollState)
        )
    }
}

class SyntaxHighlightingVisualTransformation(
    private val highlightRanges: List<Pair<IntRange, Color>>,
    private val baseColor: Color
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder(text.text)
        builder.addStyle(SpanStyle(color = baseColor), 0, text.text.length)

        highlightRanges.forEach { (range, color) ->
            val start = range.first.coerceIn(0, text.text.length)
            val end = range.last.coerceAtMost(text.text.length -1) + 1

            if (start < end) {
                builder.addStyle(SpanStyle(color = color), start, end)
            }
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CodeEditor(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    codeEditorModifier: Modifier = Modifier.fillMaxSize(),
    textLayoutResultState: MutableState<TextLayoutResult?> = remember { mutableStateOf(null) },
    highlightColor: Color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f),
    editorTextStyle: TextStyle = LocalTextStyle.current.copy(
        color = MaterialTheme.colors.onSurface,
        fontFamily = FontFamily.Monospace
    ),
    highlightRanges: List<Pair<IntRange, Color>> = emptyList()
) {
    val currentTextLayout = textLayoutResultState.value

    val editorScrollState = rememberTextFieldScrollState(Orientation.Vertical)
    val lineNumberScrollState = rememberScrollState()

    LaunchedEffect(editorScrollState) {
        snapshotFlow { editorScrollState.offset }
            .distinctUntilChanged()
            .collect { offset ->
                lineNumberScrollState.scrollTo(offset.toInt())
            }
    }

    val cursorLine = remember(textFieldValue.selection, currentTextLayout) {
        currentTextLayout?.let { layoutResult ->
            if (textFieldValue.text.isNotEmpty()) {
                val offset = textFieldValue.selection.start.coerceIn(0, textFieldValue.text.length)
                try {
                    layoutResult.getLineForOffset(offset)
                } catch (e: IndexOutOfBoundsException) {
                    textFieldValue.text.substring(0, offset.coerceAtMost(textFieldValue.text.length)).count { it == '\n' }
                }
            } else {
                0
            }
        } ?: 0
    }

    val visualTransformation = remember(highlightRanges, editorTextStyle.color) {
        SyntaxHighlightingVisualTransformation(highlightRanges, editorTextStyle.color)
    }

    Row(modifier = modifier) {
        LineNumberColumn(
            textLayoutResultState = textLayoutResultState,
            lineNumberScrollState = lineNumberScrollState,
            textStyle = editorTextStyle,
        )

        Spacer(Modifier.width(8.dp))

        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = onValueChange,
                modifier = codeEditorModifier,
                textStyle = editorTextStyle,
                cursorBrush = SolidColor(MaterialTheme.colors.primary),
                scrollState = editorScrollState,
                onTextLayout = { textLayoutResult ->
                    textLayoutResultState.value = textLayoutResult
                },
                visualTransformation = visualTransformation,
                decorationBox = { innerTextField ->
                    Box {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            currentTextLayout?.let { layout ->
                                if (cursorLine >= 0 && cursorLine < layout.lineCount) {
                                    val scrollOffset = editorScrollState.offset

                                    val lineTop = layout.getLineTop(cursorLine) - scrollOffset
                                    val lineBottom = layout.getLineBottom(cursorLine) - scrollOffset
                                    val lineWidth = size.width

                                    if (lineBottom > 0 && lineTop < size.height) {
                                        drawRect(
                                            color = highlightColor,
                                            topLeft = Offset(0f, lineTop),
                                            size = Size(lineWidth, (lineBottom - lineTop))
                                        )
                                    }
                                }
                            }
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

@Composable
fun Ide(code: String) {
    var textState by remember {
        mutableStateOf(
            TextFieldValue(
                code,
                selection = TextRange(0)
            )
        )
    }

    val highlights = remember(textState.text) {
//        generateHighlights(textState.text)
//        List<Pair<IntRange, Color>>(0) {
//            Pair(0..0, Color.Blue)
//        }
        KotlinSyntaxHighlighter().highlight(textState.text).map { (range, color) ->
            Pair(range, color.color)
        }
    }

    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
            CodeEditor(
                textFieldValue = textState,
                onValueChange = { textState = it },
                modifier = Modifier.fillMaxSize().padding(16.dp),
                codeEditorModifier = Modifier.fillMaxSize(),
                remember { mutableStateOf(null) },
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
}

val DEFAULT_CODE = """
/**
 * This is a KDoc block comment, used for demonstrating
 * multi-line comment highlighting.
 */
package com.example.showcase

import java.util.Date

private const val GREETING = "Processing user:"

enum class UserStatus {
    ACTIVE, INACTIVE, BANNED
}

data class User(val id: Long, val username: String, val status: UserStatus)

class SyntaxShowcase {

    private var processCount = 0
    private val processingFee = 1.25f // A float literal

    /**
     * Processes a user, returning a status message.
     */
    fun processUser(user: User): String {
        processCount += 1
        val creationTime = Date().time

        if (user.id > 0 && user.status != UserStatus.BANNED) {
            val message = when (user.status) {
                UserStatus.ACTIVE -> "Welcome, user.username!"
                UserStatus.INACTIVE -> "Please re-activate your account."
            }
            return "GREETING message (Processed at creationTime)"
        } else {
            return "Cannot process banned or invalid user."
        }
    }
    
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            // This is a single-line comment.
            val showcase = SyntaxShowcase()
            val activeUser = User(101L, "alex_dev", UserStatus.ACTIVE)
            
            val result = showcase.processUser(activeUser)
            println(result)
        }
    }
}
""".trimIndent()

fun main() = application {
    Window(
        title = "Code Editor",
        state = rememberWindowState(),
        onCloseRequest = ::exitApplication
    ) {
        Ide(DEFAULT_CODE)
    }
}
