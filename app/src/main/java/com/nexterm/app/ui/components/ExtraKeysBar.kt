package com.nexterm.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexterm.app.ui.theme.TerminalColorScheme

@Composable
fun ExtraKeysBar(
    keysRow: String,
    onKeyClick: (String) -> Unit,
    colorScheme: TerminalColorScheme,
    modifier: Modifier = Modifier
) {
    val keys = remember(keysRow) { keysRow.split("|") }
    val scrollState = rememberScrollState()

    var ctrlPressed by remember { mutableStateOf(false) }
    var altPressed by remember { mutableStateOf(false) }
    var fnPressed by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colorScheme.background.copy(alpha = 0.95f))
            .horizontalScroll(scrollState)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        keys.forEach { key ->
            ExtraKeyButton(
                key = key,
                colorScheme = colorScheme,
                isModifierActive = when (key.uppercase()) {
                    "CTRL" -> ctrlPressed
                    "ALT" -> altPressed
                    "FN" -> fnPressed
                    else -> false
                },
                onClick = {
                    when (key.uppercase()) {
                        "CTRL" -> {
                            ctrlPressed = !ctrlPressed
                        }
                        "ALT" -> {
                            altPressed = !altPressed
                        }
                        "FN" -> {
                            fnPressed = !fnPressed
                        }
                        else -> {
                            val modifiedKey = buildString {
                                if (ctrlPressed) append("CTRL+")
                                if (altPressed) append("ALT+")
                                append(key.uppercase())
                            }
                            onKeyClick(modifiedKey)
                            ctrlPressed = false
                            altPressed = false
                            fnPressed = false
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ExtraKeyButton(
    key: String,
    colorScheme: TerminalColorScheme,
    isModifierActive: Boolean = false,
    onClick: () -> Unit
) {
    val displayText = when (key.uppercase()) {
        "ESC" -> "ESC"
        "TAB" -> "TAB"
        "CTRL" -> "CTRL"
        "ALT" -> "ALT"
        "FN" -> "FN"
        "HOME" -> "HOME"
        "END" -> "END"
        "PGUP" -> "PGUP"
        "PGDN" -> "PGDN"
        "UP" -> "↑"
        "DOWN" -> "↓"
        "LEFT" -> "←"
        "RIGHT" -> "→"
        else -> key
    }

    val isModifier = key.uppercase() in listOf("CTRL", "ALT", "FN")

    Button(
        onClick = onClick,
        modifier = Modifier
            .height(36.dp)
            .widthIn(min = 40.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isModifierActive) {
                colorScheme.green.copy(alpha = 0.3f)
            } else {
                colorScheme.black.copy(alpha = 0.6f)
            },
            contentColor = if (isModifierActive) {
                colorScheme.green
            } else {
                colorScheme.foreground
            }
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = displayText,
            fontSize = 11.sp,
            fontWeight = if (isModifier) FontWeight.Bold else FontWeight.Normal,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )
    }
}