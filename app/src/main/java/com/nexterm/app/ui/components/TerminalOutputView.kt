package com.nexterm.app.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexterm.app.terminal.buffer.TerminalLine
import com.nexterm.app.ui.theme.TerminalColorScheme
import kotlin.math.max

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
    val density = LocalDensity.current
    
    // Filter out trailing blank lines
    val displayLines = remember(lines) {
        val lastContentIndex = lines.indexOfLast { !it.isBlank() }
        if (lastContentIndex == -1) {
            if (lines.isEmpty()) emptyList() else listOf(lines[0])
        } else {
            lines.take(lastContentIndex + 1)
        }
    }

    // Dynamic width calculation based on the longest line
    // Since it's Monospace, char width is consistent (approx 0.6 * fontSize)
    val estimatedWidth = remember(displayLines, fontSize) {
        val maxChars = displayLines.maxOfOrNull { it.getText().trimEnd().length } ?: 0
        with(density) {
            // 0.6f is a standard ratio for monospace width/height
            (maxChars * fontSize * 0.62f).sp.toDp() + 32.dp 
        }
    }

    // Auto-scroll to bottom
    LaunchedEffect(displayLines.size) {
        if (displayLines.isNotEmpty()) {
            listState.animateScrollToItem(displayLines.lastIndex)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .horizontalScroll(horizontalScrollState)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxHeight()
                .width(estimatedWidth) // Apply the flexible calculated width
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .pointerInput(displayLines) {
                    detectTapGestures(
                        onTap = { onTap() },
                        onLongPress = {
                            val text = buildString {
                                displayLines.forEach { line ->
                                    append(line.getText().trimEnd())
                                    append("\n")
                                }
                            }.trimEnd()

                            if (text.isNotEmpty()) {
                                clipboardManager.setText(AnnotatedString(text))
                            }
                        }
                    )
                }
        ) {
            items(
                items = displayLines,
                key = { it.id }
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
                append(" ")
                return@buildAnnotatedString
            }

            chars.forEach { terminalChar ->
                val attrs = terminalChar.attributes

                val fgColor = when {
                    attrs.inverse -> colorScheme.getColor(attrs.background)
                    attrs.foreground in 0..15 -> colorScheme.getColor(attrs.foreground)
                    attrs.foreground in 16..255 -> get256Color(attrs.foreground)
                    else -> colorScheme.foreground
                }

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
                        attrs.dim -> fgColor.copy(alpha = 0.55f)
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

                withStyle(style) {
                    append(terminalChar.char.toString())
                }
            }
        }
    }

    Text(
        text = annotatedString,
        fontFamily = FontFamily.Monospace,
        fontSize = fontSize.sp,
        lineHeight = (fontSize * 1.32f).sp,
        softWrap = false
    )
}

private fun get256Color(index: Int): Color {
    return when {
        index < 16 -> {
            when (index) {
                0 -> Color(0xFF000000)
                1 -> Color(0xFF800000)
                2 -> Color(0xFF008000)
                3 -> Color(0xFF808000)
                4 -> Color(0xFF000080)
                5 -> Color(0xFF800080)
                6 -> Color(0xFF008080)
                7 -> Color(0xFFC0C0C0)
                8 -> Color(0xFF808080)
                9 -> Color(0xFFFF0000)
                10 -> Color(0xFF00FF00)
                11 -> Color(0xFFFFFF00)
                12 -> Color(0xFF0000FF)
                13 -> Color(0xFFFF00FF)
                14 -> Color(0xFF00FFFF)
                else -> Color(0xFFFFFFFF)
            }
        }

        index in 16..231 -> {
            val i = index - 16
            val r = (i / 36) % 6
            val g = (i / 6) % 6
            val b = i % 6

            fun level(value: Int): Float {
                return if (value == 0) 0f else (55 + value * 40) / 255f
            }

            Color(
                red = level(r),
                green = level(g),
                blue = level(b)
            )
        }

        else -> {
            val gray = ((index - 232) * 10 + 8).coerceIn(0, 255)
            Color(gray / 255f, gray / 255f, gray / 255f)
        }
    }
}
