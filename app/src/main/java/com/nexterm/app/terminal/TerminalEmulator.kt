package com.nexterm.app.terminal

import android.util.Log
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
        Log.e("TerminalEmulator", "TerminalEmulator.init() called, rows=$rows, cols=$cols")
        updateScreen()
    }

    fun write(data: String) {
        Log.e("TerminalEmulator", "write() called with data length=${data.length}, content: $data")
        if (data.isEmpty()) {
            return
        }
        val parsed = ansiParser.parse(data)
        Log.e("TerminalEmulator", "Parsed ${parsed.size} sequences")
        parsed.forEach { seq ->
            when (seq) {
                is AnsiParser.ParsedSequence.Text -> {
                    Log.e("TerminalEmulator", "Text sequence: ${seq.text}")
                    seq.text.forEach { writeChar(it) }
                }
                is AnsiParser.ParsedSequence.ControlCode -> {
                    Log.e("TerminalEmulator", "Control code: ${seq.code}")
                    handleControlCode(seq.code)
                }
                is AnsiParser.ParsedSequence.EscapeSequence -> {
                    Log.e("TerminalEmulator", "Escape sequence: ${seq.type} params=${seq.params}")
                    handleEscapeSequence(seq)
                }
            }
        }
        updateScreen()
        Log.e("TerminalEmulator", "updateScreen() called, screenContent now has ${_screenContent.value.size} lines")
    }

    private fun writeChar(char: Char) {
        if (_cursorPosition.value.col >= cols) { newLine() }
        val cursor = _cursorPosition.value
        _buffer.value.setChar(cursor.row, cursor.col, char, currentAttributes)
        _cursorPosition.value = cursor.copy(col = cursor.col + 1)
    }

    private fun handleControlCode(code: Char) {
        when (code) {
            '\n' -> newLine()
            '\r' -> carriageReturn()
            '\t' -> repeat(8 - (_cursorPosition.value.col % 8)) { writeChar(' ') }
            '\b' -> _cursorPosition.value = _cursorPosition.value.copy(col = (_cursorPosition.value.col - 1).coerceAtLeast(0))
        }
    }

    private fun handleEscapeSequence(seq: AnsiParser.ParsedSequence.EscapeSequence) {
        when (seq.type) {
            "m" -> handleSGR(seq.params)
            "H", "f" -> {
                val r = (seq.params.getOrNull(0) ?: 1) - 1
                val c = (seq.params.getOrNull(1) ?: 1) - 1
                _cursorPosition.value = CursorPosition(r.coerceIn(0, rows - 1), c.coerceIn(0, cols - 1))
            }
            "J" -> _buffer.value.clear()
            "K" -> _buffer.value.clearLine(_cursorPosition.value.row, _cursorPosition.value.col, cols - 1)
        }
    }

    private fun handleSGR(params: List<Int>) {
        if (params.isEmpty()) { currentAttributes = CharacterAttributes(); return }
        var i = 0
        while (i < params.size) {
            when (val p = params[i]) {
                0 -> currentAttributes = CharacterAttributes()
                1 -> currentAttributes = currentAttributes.copy(bold = true)
                2 -> currentAttributes = currentAttributes.copy(dim = true)
                3 -> currentAttributes = currentAttributes.copy(italic = true)
                4 -> currentAttributes = currentAttributes.copy(underline = true)
                7 -> currentAttributes = currentAttributes.copy(inverse = true)
                8 -> currentAttributes = currentAttributes.copy(hidden = true)
                9 -> currentAttributes = currentAttributes.copy(strikethrough = true)
                22 -> currentAttributes = currentAttributes.copy(bold = false, dim = false)
                23 -> currentAttributes = currentAttributes.copy(italic = false)
                24 -> currentAttributes = currentAttributes.copy(underline = false)
                27 -> currentAttributes = currentAttributes.copy(inverse = false)
                28 -> currentAttributes = currentAttributes.copy(hidden = false)
                29 -> currentAttributes = currentAttributes.copy(strikethrough = false)
                in 30..37 -> currentAttributes = currentAttributes.copy(foreground = p - 30)
                38 -> {
                    if (i + 2 < params.size && params[i + 1] == 5) {
                        currentAttributes = currentAttributes.copy(foreground = params[i + 2])
                        i += 2
                    }
                }
                39 -> currentAttributes = currentAttributes.copy(foreground = 7)
                in 40..47 -> currentAttributes = currentAttributes.copy(background = p - 40)
                48 -> {
                    if (i + 2 < params.size && params[i + 1] == 5) {
                        currentAttributes = currentAttributes.copy(background = params[i + 2])
                        i += 2
                    }
                }
                49 -> currentAttributes = currentAttributes.copy(background = 0)
                in 90..97 -> currentAttributes = currentAttributes.copy(foreground = p - 90 + 8)
                in 100..107 -> currentAttributes = currentAttributes.copy(background = p - 100 + 8)
            }
            i++
        }
    }

    private fun newLine() {
        val cursor = _cursorPosition.value
        if (cursor.row >= rows - 1) {
            val topRow = _buffer.value.getLine(0)
            if (topRow != null) {
                scrollbackBuffer.add(topRow.copy())
                if (scrollbackBuffer.size > maxScrollback) scrollbackBuffer.removeAt(0)
            }
            _buffer.value.scrollUp()
            _cursorPosition.value = cursor.copy(col = 0)
        } else {
            _cursorPosition.value = cursor.copy(row = cursor.row + 1, col = 0)
        }
        // Force trigger buffer refresh
        _buffer.value = _buffer.value.copy()
    }

    private fun carriageReturn() { _cursorPosition.value = _cursorPosition.value.copy(col = 0) }

    private fun updateScreen() {
        // 🔥 FIX: Create a deep copy of the lines. This creates new TerminalLine objects,
        // ensuring the StateFlow emits a new value and the UI updates.
        val newContent = (scrollbackBuffer + _buffer.value.getLines()).map { it.copy() }
        Log.e("TerminalEmulator", "updateScreen(): total lines=${newContent.size}, scrollback=${scrollbackBuffer.size}, buffer=${_buffer.value.getLines().size}")
        newContent.forEachIndexed { index, line ->
            Log.e("TerminalEmulator", "  Line[$index]: '${line.getText()}'")
        }
        _screenContent.value = newContent
    }

    fun resize(r: Int, c: Int) {
        rows = r; cols = c
        _buffer.value = _buffer.value.resize(r, c)
        updateScreen()
    }
}

data class CursorPosition(val row: Int, val col: Int)

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