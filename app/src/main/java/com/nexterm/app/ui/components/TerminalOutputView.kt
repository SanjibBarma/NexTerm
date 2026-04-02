package com.nexterm.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexterm.app.terminal.buffer.TerminalLine
import com.nexterm.app.ui.theme.TerminalColorScheme

@Composable
fun TerminalOutputView(
    lines: List<TerminalLine>,
    colorScheme: TerminalColorScheme,
    fontSize: Int,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {}
) {
    val listState = rememberLazyListState()
    val horizontalScrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current

    var selectedText by remember { mutableStateOf("") }

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.lastIndex)
        }
    }

    LaunchedEffect(lines) {
        if (lines.isNotEmpty()) {
            // Scroll to bottom
            listState.animateScrollToItem(lines.lastIndex)

            // Print the terminal output
            printTerminalOutput(lines)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = {
                        // Copy all visible text to clipboard
                        val text = lines.joinToString("\n") { it.getText().trimEnd() }
                        clipboardManager.setText(AnnotatedString(text))
                    }
                )
            }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(horizontalScrollState)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            items(
                items = lines,
                key = { it.hashCode() }
            ) { line ->
                TerminalLineView(
                    line = line,
                    colorScheme = colorScheme,
                    fontSize = fontSize
                )
            }
        }
    }
}

@Composable
fun TerminalLineView(
    line: TerminalLine,
    colorScheme: TerminalColorScheme,
    fontSize: Int
) {
    val annotatedString = remember(line, colorScheme) {
        buildAnnotatedString {
            val chars = line.getChars()
            if (chars.isEmpty()) {
                // Empty line, add a space to maintain line height
                append(" ")
                return@buildAnnotatedString
            }

            chars.forEach { terminalChar ->
                val attrs = terminalChar.attributes

                // Determine foreground color
                val fgColor = when {
                    attrs.inverse -> colorScheme.getColor(attrs.background)
                    attrs.foreground in 0..15 -> colorScheme.getColor(attrs.foreground)
                    attrs.foreground in 16..255 -> get256Color(attrs.foreground)
                    else -> colorScheme.foreground
                }

                // Determine background color
                val bgColor = when {
                    attrs.inverse -> colorScheme.getColor(attrs.foreground)
                    attrs.background == 0 -> Color.Transparent
                    attrs.background in 1..15 -> colorScheme.getColor(attrs.background)
                    attrs.background in 16..255 -> get256Color(attrs.background)
                    else -> Color.Transparent
                }

                val style = SpanStyle(
                    color = when {
                        attrs.hidden -> Color.Transparent
                        attrs.dim -> fgColor.copy(alpha = 0.5f)
                        else -> fgColor
                    },
                    background = bgColor,
                    fontWeight = if (attrs.bold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (attrs.italic) FontStyle.Italic else FontStyle.Normal,
                    textDecoration = when {
                        attrs.underline && attrs.strikethrough -> TextDecoration.combine(
                            listOf(TextDecoration.Underline, TextDecoration.LineThrough)
                        )
                        attrs.underline -> TextDecoration.Underline
                        attrs.strikethrough -> TextDecoration.LineThrough
                        else -> TextDecoration.None
                    }
                )

                append(AnnotatedString(terminalChar.char.toString(), style))
            }
        }
    }

    Text(
        text = annotatedString,
        fontFamily = FontFamily.Monospace,
        fontSize = fontSize.sp,
        lineHeight = (fontSize * 1.4).sp,
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Get 256-color palette color
 */
private fun get256Color(index: Int): Color {
    return when {
        index < 16 -> {
            // Standard colors (handled by colorScheme)
            Color.White
        }
        index < 232 -> {
            // 216 color cube (6x6x6)
            val i = index - 16
            val r = (i / 36) % 6
            val g = (i / 6) % 6
            val b = i % 6
            Color(
                red = if (r == 0) 0f else (55 + r * 40) / 255f,
                green = if (g == 0) 0f else (55 + g * 40) / 255f,
                blue = if (b == 0) 0f else (55 + b * 40) / 255f
            )
        }
        else -> {
            // 24 grayscale colors
            val gray = (index - 232) * 10 + 8
            Color(gray / 255f, gray / 255f, gray / 255f)
        }
    }
}

fun printTerminalOutput(lines: List<TerminalLine>) {
    val output = buildString {
        lines.forEach { line ->
            line.getChars().forEach { terminalChar ->
                append(terminalChar.char)
            }
            append("\n") // newline at the end of each line
        }
    }
    println("terminal_output: $output")
}