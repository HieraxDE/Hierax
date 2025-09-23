package app.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import app.data.Suggestion
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.roundToInt

@Composable
fun SuggestionPopup(
    suggestions: List<Suggestion>,
    onSuggestionClicked: (Suggestion) -> Unit,
    onDismissRequest: () -> Unit,
    offset: IntOffset = IntOffset(0, 0),
    selectedIndex: Int
) {
    if (suggestions.isEmpty()) {
        return
    }

    val listState = rememberLazyListState()

    LaunchedEffect(selectedIndex) {
        if (selectedIndex != -1) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    Popup(
        alignment = Alignment.TopStart,
        onDismissRequest = onDismissRequest,
        offset = offset,
        focusable = false
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            shadowElevation = 8.dp,
            modifier = Modifier.width(280.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.heightIn(max = 240.dp)
            ) {
                itemsIndexed(suggestions, key = { _, item -> item.id }) { index, suggestion ->
                    val isSelected = index == selectedIndex
                    SuggestionItem(
                        suggestion = suggestion,
                        isSelected = isSelected,
                        onClick = { onSuggestionClicked(suggestion) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionItem(
    suggestion: Suggestion,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = suggestion.icon,
            contentDescription = null, // Decorative
            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = suggestion.name,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        )
    }
}

val allUsers = listOf(
    Suggestion("user1", "Alice", Icons.Default.Person),
    Suggestion("user2", "Bob", Icons.Default.Face),
    Suggestion("user3", "Charles", Icons.Default.AccountCircle),
    Suggestion("user4", "Diana", Icons.Default.SupervisorAccount),
    Suggestion("user5", "Ethan", Icons.Default.EmojiPeople),
    Suggestion("user6", "Fiona", Icons.Default.FaceRetouchingNatural),
    Suggestion("user7", "George", Icons.Default.Accessibility),
)

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Suggestion Popup Demo") {
        MaterialTheme {
            SuggestionPopupDemo()
        }
    }
}

@Composable
@Preview
fun SuggestionPopupDemo() {
    var text by remember { mutableStateOf("") }
    var textFieldSize by remember { mutableStateOf(IntSize.Zero) }
    var isPopupVisible by remember { mutableStateOf(false) }

    var selectedIndex by remember { mutableStateOf(-1) }

    val filteredSuggestions = remember(text) {
        if (text.isNotBlank()) {
            allUsers.filter { it.name.contains(text, ignoreCase = true) }
        } else {
            emptyList()
        }
    }

    LaunchedEffect(text, filteredSuggestions) {
        if (text.isNotBlank() && filteredSuggestions.isNotEmpty()) {
            isPopupVisible = true
            selectedIndex = 0
        } else {
            isPopupVisible = false
            selectedIndex = -1
        }
    }

    val onSelect: (Suggestion) -> Unit = { suggestion ->
        text = suggestion.name
        isPopupVisible = false
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Type a name to get suggestions:", style = MaterialTheme.typography.titleMedium)

        Box {
            OutlinedTextField(
                value = text,
                onValueChange = { newText ->
                    text = newText
                },
                label = { Text("Search Users") },
                modifier = Modifier
                    .onGloballyPositioned { coordinates ->
                        textFieldSize = coordinates.size
                    }
                    .onPreviewKeyEvent { event ->
                        if (!isPopupVisible || filteredSuggestions.isEmpty()) {
                            return@onPreviewKeyEvent false
                        }

                        if (event.type == KeyEventType.KeyDown) {
                            when (event.key) {
                                Key.DirectionDown -> {
                                    selectedIndex = (selectedIndex + 1) % filteredSuggestions.size
                                    return@onPreviewKeyEvent true
                                }
                                Key.DirectionUp -> {
                                    selectedIndex = (selectedIndex - 1 + filteredSuggestions.size) % filteredSuggestions.size
                                    return@onPreviewKeyEvent true
                                }
                                Key.Enter -> {
                                    if (selectedIndex != -1) {
                                        onSelect(filteredSuggestions[selectedIndex])
                                    }
                                    return@onPreviewKeyEvent true
                                }
                            }
                        }
                        false
                    }
            )

            if (isPopupVisible) {
                val density = LocalDensity.current
                val popupYOffset = with(density) { textFieldSize.height.toDp() + 4.dp }

                SuggestionPopup(
                    suggestions = filteredSuggestions,
                    onSuggestionClicked = { suggestion ->
                        onSelect(suggestion)
                    },
                    onDismissRequest = {
                        isPopupVisible = false
                    },
                    offset = IntOffset(0, popupYOffset.value.roundToInt()),
                    selectedIndex = selectedIndex
                )
            }
        }
    }
}
