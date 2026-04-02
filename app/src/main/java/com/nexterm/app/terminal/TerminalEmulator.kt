package com.nexterm.app.terminal

import com.nexterm.app.terminal.ansi.AnsiParser
import com.nexterm.app.terminal.buffer.TerminalBuffer
import com.nexterm.app.terminal.buffer.TerminalLine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TerminalEmulator(
    private var rows: Int,
    private var cols: Int,
    private val ansiParser: AnsiParser
) {
    private val _buffer = MutableStateFlow(TerminalBuffer(rows, cols))
    val buffer: StateFlow<TerminalBuffer> = _buffer.asStateFlow()

    private val _cursorPosition = MutableStateFlow(CursorPosition(0, 0))
    val cursorPosition: StateFlow<CursorPosition> = _cursorPosition.asStateFlow()

    private val _screenContent = MutableStateFlow<List<TerminalLine>>(emptyList())
    val screenContent: StateFlow<List<TerminalLine>> = _screenContent.asStateFlow()

    private var currentAttributes = CharacterAttributes()
    private var savedCursorPosition = CursorPosition(0, 0)

    private val scrollbackBuffer = mutableListOf<TerminalLine>()
    private var maxScrollback = 10000

    init {
        updateScreen()
    }

    fun write(data: String) {
        val parsedSequences = ansiParser.parse(data)

        parsedSequences.forEach { sequence ->
            when (sequence) {
                is AnsiParser.ParsedSequence.Text -> writeText(sequence.text)
                is AnsiParser.ParsedSequence.ControlCode -> handleControlCode(sequence.code)
                is AnsiParser.ParsedSequence.EscapeSequence -> handleEscapeSequence(sequence)
            }
        }

        updateScreen()
    }

    private fun writeText(text: String) {
        text.forEach { char ->
            writeChar(char)
        }
    }

    private fun writeChar(char: Char) {
        var cursor = _cursorPosition.value
        var buffer = _buffer.value

        if (cursor.col >= cols) {
            newLine()
            cursor = _cursorPosition.value
            buffer = _buffer.value
        }

        buffer.setChar(cursor.row, cursor.col, char, currentAttributes)
        _buffer.value = buffer.copy()

        _cursorPosition.value = cursor.copy(col = cursor.col + 1)
    }

    private fun handleControlCode(code: Char) {
        when (code) {
            '\n' -> newLine()
            '\r' -> carriageReturn()
            '\t' -> tab()
            '\b' -> backspace()
            '\u0007' -> bell()
        }
    }

    private fun handleEscapeSequence(sequence: AnsiParser.ParsedSequence.EscapeSequence) {
        when (sequence.type) {
            "m" -> handleSGR(sequence.params)
            "H", "f" -> handleCursorPosition(sequence.params)
            "A" -> moveCursorUp(sequence.params.firstOrNull() ?: 1)
            "B" -> moveCursorDown(sequence.params.firstOrNull() ?: 1)
            "C" -> moveCursorForward(sequence.params.firstOrNull() ?: 1)
            "D" -> moveCursorBack(sequence.params.firstOrNull() ?: 1)
            "J" -> handleEraseDisplay(sequence.params.firstOrNull() ?: 0)
            "K" -> handleEraseLine(sequence.params.firstOrNull() ?: 0)
            "s" -> saveCursor()
            "u" -> restoreCursor()
            "r" -> setScrollRegion(sequence.params)
            "IND" -> newLine()
            "NEL" -> {
                newLine()
                carriageReturn()
            }
            "RI" -> reverseIndex()
            "RIS" -> reset()
        }
    }

    private fun handleSGR(params: List<Int>) {
        if (params.isEmpty()) {
            currentAttributes = CharacterAttributes()
            return
        }

        var i = 0
        while (i < params.size) {
            when (val param = params[i]) {
                0 -> currentAttributes = CharacterAttributes()
                1 -> currentAttributes = currentAttributes.copy(bold = true)
                2 -> currentAttributes = currentAttributes.copy(dim = true)
                3 -> currentAttributes = currentAttributes.copy(italic = true)
                4 -> currentAttributes = currentAttributes.copy(underline = true)
                5, 6 -> currentAttributes = currentAttributes.copy(blink = true)
                7 -> currentAttributes = currentAttributes.copy(inverse = true)
                8 -> currentAttributes = currentAttributes.copy(hidden = true)
                9 -> currentAttributes = currentAttributes.copy(strikethrough = true)
                22 -> currentAttributes = currentAttributes.copy(bold = false, dim = false)
                23 -> currentAttributes = currentAttributes.copy(italic = false)
                24 -> currentAttributes = currentAttributes.copy(underline = false)
                25 -> currentAttributes = currentAttributes.copy(blink = false)
                27 -> currentAttributes = currentAttributes.copy(inverse = false)
                28 -> currentAttributes = currentAttributes.copy(hidden = false)
                29 -> currentAttributes = currentAttributes.copy(strikethrough = false)
                in 30..37 -> currentAttributes = currentAttributes.copy(foreground = param - 30)
                38 -> {
                    if (i + 2 < params.size && params[i + 1] == 5) {
                        currentAttributes = currentAttributes.copy(foreground = params[i + 2])
                        i += 2
                    }
                }
                39 -> currentAttributes = currentAttributes.copy(foreground = 7)
                in 40..47 -> currentAttributes = currentAttributes.copy(background = param - 40)
                48 -> {
                    if (i + 2 < params.size && params[i + 1] == 5) {
                        currentAttributes = currentAttributes.copy(background = params[i + 2])
                        i += 2
                    }
                }
                49 -> currentAttributes = currentAttributes.copy(background = 0)
                in 90..97 -> currentAttributes = currentAttributes.copy(foreground = param - 90 + 8)
                in 100..107 -> currentAttributes = currentAttributes.copy(background = param - 100 + 8)
            }
            i++
        }
    }

    private fun handleCursorPosition(params: List<Int>) {
        val row = (params.getOrNull(0) ?: 1) - 1
        val col = (params.getOrNull(1) ?: 1) - 1
        _cursorPosition.value = CursorPosition(
            row = row.coerceIn(0, rows - 1),
            col = col.coerceIn(0, cols - 1)
        )
    }

    private fun moveCursorUp(n: Int) {
        val cursor = _cursorPosition.value
        _cursorPosition.value = cursor.copy(row = (cursor.row - n).coerceAtLeast(0))
    }

    private fun moveCursorDown(n: Int) {
        val cursor = _cursorPosition.value
        _cursorPosition.value = cursor.copy(row = (cursor.row + n).coerceAtMost(rows - 1))
    }

    private fun moveCursorForward(n: Int) {
        val cursor = _cursorPosition.value
        _cursorPosition.value = cursor.copy(col = (cursor.col + n).coerceAtMost(cols - 1))
    }

    private fun moveCursorBack(n: Int) {
        val cursor = _cursorPosition.value
        _cursorPosition.value = cursor.copy(col = (cursor.col - n).coerceAtLeast(0))
    }

    private fun handleEraseDisplay(mode: Int) {
        val buffer = _buffer.value
        val cursor = _cursorPosition.value

        when (mode) {
            0 -> buffer.clearRange(cursor.row, cursor.col, rows - 1, cols - 1)
            1 -> buffer.clearRange(0, 0, cursor.row, cursor.col)
            2, 3 -> buffer.clear()
        }

        _buffer.value = buffer.copy()
    }

    private fun handleEraseLine(mode: Int) {
        val buffer = _buffer.value
        val cursor = _cursorPosition.value

        when (mode) {
            0 -> buffer.clearLine(cursor.row, cursor.col, cols - 1)
            1 -> buffer.clearLine(cursor.row, 0, cursor.col)
            2 -> buffer.clearLine(cursor.row, 0, cols - 1)
        }

        _buffer.value = buffer.copy()
    }

    private fun setScrollRegion(params: List<Int>) {
        val top = (params.getOrNull(0) ?: 1) - 1
        val bottom = (params.getOrNull(1) ?: rows) - 1
        val buffer = _buffer.value
        buffer.setScrollRegion(top, bottom)
        _buffer.value = buffer.copy()
    }

    private fun saveCursor() {
        savedCursorPosition = _cursorPosition.value
    }

    private fun restoreCursor() {
        _cursorPosition.value = savedCursorPosition
    }

    private fun newLine() {
        val cursor = _cursorPosition.value

        if (cursor.row >= rows - 1) {
            scroll()
        } else {
            _cursorPosition.value = cursor.copy(
                row = cursor.row + 1,
                col = 0
            )
        }
    }

    private fun carriageReturn() {
        _cursorPosition.value = _cursorPosition.value.copy(col = 0)
    }

    private fun tab() {
        val cursor = _cursorPosition.value
        val nextTab = ((cursor.col / 8) + 1) * 8
        _cursorPosition.value = cursor.copy(col = nextTab.coerceAtMost(cols - 1))
    }

    private fun backspace() {
        val cursor = _cursorPosition.value
        if (cursor.col > 0) {
            _cursorPosition.value = cursor.copy(col = cursor.col - 1)
        }
    }

    private fun bell() {
        // optional bell hook
    }

    private fun reverseIndex() {
        val cursor = _cursorPosition.value
        _cursorPosition.value = cursor.copy(row = (cursor.row - 1).coerceAtLeast(0))
    }

    private fun reset() {
        _buffer.value = TerminalBuffer(rows, cols)
        _cursorPosition.value = CursorPosition(0, 0)
        currentAttributes = CharacterAttributes()
        savedCursorPosition = CursorPosition(0, 0)
        scrollbackBuffer.clear()
        updateScreen()
    }

    private fun scroll() {
        val buffer = _buffer.value
        val firstLine = buffer.getLine(0)

        if (firstLine != null) {
            scrollbackBuffer.add(firstLine.copy())
            while (scrollbackBuffer.size > maxScrollback) {
                scrollbackBuffer.removeAt(0)
            }
        }

        buffer.scrollUp()
        _buffer.value = buffer.copy()
        _cursorPosition.value = _cursorPosition.value.copy(col = 0)
    }

    private fun updateScreen() {
        val visibleLines = _buffer.value.getDisplayLines()

        val combinedLines = buildList {
            addAll(scrollbackBuffer.map { it.copy() })
            addAll(visibleLines.map { it.copy() })
        }

        _screenContent.value = combinedLines.takeLast(maxScrollback + rows)
    }

    fun resize(newRows: Int, newCols: Int) {
        rows = newRows
        cols = newCols

        _buffer.value = _buffer.value.resize(newRows, newCols)
        _cursorPosition.value = CursorPosition(
            row = _cursorPosition.value.row.coerceIn(0, newRows - 1),
            col = _cursorPosition.value.col.coerceIn(0, newCols - 1)
        )

        updateScreen()
    }

    fun getScrollbackHistory(): List<TerminalLine> = scrollbackBuffer.toList()

    fun setMaxScrollback(lines: Int) {
        maxScrollback = lines.coerceAtLeast(100)
        while (scrollbackBuffer.size > maxScrollback) {
            scrollbackBuffer.removeAt(0)
        }
        updateScreen()
    }
}

data class CursorPosition(
    val row: Int,
    val col: Int
)

data class CharacterAttributes(
    val bold: Boolean = false,
    val dim: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val blink: Boolean = false,
    val inverse: Boolean = false,
    val hidden: Boolean = false,
    val strikethrough: Boolean = false,
    val foreground: Int = 7,
    val background: Int = 0
)