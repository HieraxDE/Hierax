package app.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Cursor

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        state = rememberWindowState(size = DpSize(1000.dp, 600.dp)),
        title = "split"
    ) {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                val panes = listOf<@Composable () -> Unit>(
                    { PaneContent(text = "Pane 1", color = Color(0xFFB3E5FC)) },
                    { PaneContent(text = "Pane 2", color = Color(0xFFC8E6C9)) },
                    { PaneContent(text = "Pane 3", color = Color(0xFFFFCCBC)) }
                )

                MultiSplitPaneLayout(
                    modifier = Modifier.fillMaxSize(),
                    orientation = Orientation.Horizontal,
                    panes = panes
                )
            }
        }
    }
}

@Composable
fun PaneContent(text: String, color: Color) {
    Box(
        modifier = Modifier.fillMaxSize().background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 24.sp, color = Color.Black.copy(alpha = 0.8f))
    }
}

@Composable
fun MultiSplitPaneLayout(
    modifier: Modifier = Modifier,
    orientation: Orientation,
    minPaneSize: Dp = 40.dp,
    panes: List<@Composable () -> Unit>
) {
    require(panes.size >= 2) { "MultiSplitPaneLayout requires at least two panes." }

    val paneSizes = remember { mutableStateListOf<Dp>() }
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier) {
        val totalSize = if (orientation == Orientation.Horizontal) maxWidth else maxHeight

        if (paneSizes.isEmpty()) {
            val initialPaneSize = totalSize / panes.size
            repeat(panes.size - 1) {
                paneSizes.add(initialPaneSize)
            }
        }

        val layoutModifier = if (orientation == Orientation.Horizontal) {
            Modifier.fillMaxSize()
        } else {
            Modifier.fillMaxSize()
        }

        val container: @Composable (content: @Composable RowScope.() -> Unit) -> Unit =
            if (orientation == Orientation.Horizontal) {
                { Row(modifier = layoutModifier, content = it) }
            } else {
                { Column(modifier = layoutModifier, content = it as @Composable ColumnScope.() -> Unit) }
            }

        if (orientation == Orientation.Horizontal) {
            Row(modifier = Modifier.fillMaxSize()) {
                panes.forEachIndexed { index, pane ->
                    val paneModifier = if (index < panes.size - 1) {
                        Modifier.width(paneSizes[index])
                    } else {
                        Modifier.weight(1f)
                    }

                    Box(modifier = paneModifier.fillMaxHeight()) { pane() }

                    if (index < panes.size - 1) {
                        DraggableDivider(
                            orientation = orientation,
                            onDrag = { delta ->
                                with(density) {
                                    val deltaDp = delta.toDp()
                                    if (index == panes.size - 2) {
                                        val currentFlexiblePaneSize = totalSize - paneSizes.toList().sumOf {
                                            it.value.toInt()
                                        }.dp
                                        val availableSpace = paneSizes[index] + currentFlexiblePaneSize
                                        paneSizes[index] = (paneSizes[index] + deltaDp)
                                            .coerceIn(minPaneSize, availableSpace - minPaneSize)
                                    } else {
                                        val availableSpace = paneSizes[index] + paneSizes[index + 1]
                                        val newPane1Size = (paneSizes[index] + deltaDp)
                                            .coerceIn(minPaneSize, availableSpace - minPaneSize)
                                        paneSizes[index + 1] = availableSpace - newPane1Size
                                        paneSizes[index] = newPane1Size
                                    }
                                }
                            }
                        )
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                panes.forEachIndexed { index, pane ->
                    val paneModifier = if (index < panes.size - 1) {
                        Modifier.height(paneSizes[index])
                    } else {
                        Modifier.weight(1f)
                    }

                    Box(modifier = paneModifier.fillMaxWidth()) { pane() }

                    if (index < panes.size - 1) {
                        DraggableDivider(
                            orientation = orientation,
                            onDrag = { delta ->
                                with(density) {
                                    val deltaDp = delta.toDp()
                                    if (index == panes.size - 2) {
                                        val currentFlexiblePaneSize = totalSize - paneSizes.toList().sumOf {
                                            it.value.toInt()
                                        }.dp
                                        val availableSpace = paneSizes[index] + currentFlexiblePaneSize
                                        paneSizes[index] = (paneSizes[index] + deltaDp)
                                            .coerceIn(minPaneSize, availableSpace - minPaneSize)
                                    } else {
                                        val availableSpace = paneSizes[index] + paneSizes[index + 1]
                                        val newPane1Size = (paneSizes[index] + deltaDp)
                                            .coerceIn(minPaneSize, availableSpace - minPaneSize)
                                        paneSizes[index + 1] = availableSpace - newPane1Size
                                        paneSizes[index] = newPane1Size
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DraggableDivider(
    modifier: Modifier = Modifier,
    orientation: Orientation,
    onDrag: (Float) -> Unit
) {
    val draggableState = rememberDraggableState { delta ->
        onDrag(delta)
    }

    val dividerModifier = if (orientation == Orientation.Horizontal) {
        modifier.width(8.dp).fillMaxHeight()
            .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
    } else {
        modifier.height(8.dp).fillMaxWidth()
            .pointerHoverIcon(PointerIcon(Cursor(Cursor.N_RESIZE_CURSOR)))
    }

    Box(
        modifier = dividerModifier.draggable(
            state = draggableState,
            orientation = orientation,
        ),
        contentAlignment = Alignment.Center
    ) {
        val visualDividerModifier = if (orientation == Orientation.Horizontal) {
            Modifier.fillMaxHeight().width(2.dp)
        } else {
            Modifier.fillMaxWidth().height(2.dp)
        }
        Divider(
            modifier = visualDividerModifier,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
