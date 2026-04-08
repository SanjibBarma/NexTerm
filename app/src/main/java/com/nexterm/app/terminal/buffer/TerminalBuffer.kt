package com.nexterm.app.terminal.buffer

import com.nexterm.app.terminal.CharacterAttributes
import java.util.concurrent.atomic.AtomicLong

private val nextLineId = AtomicLong(0)

class TerminalBuffer(
    private val rows: Int,
    private val cols: Int
) {
    private val lines: MutableList<TerminalLine> = MutableList(rows) { TerminalLine(cols) }

    private var scrollTop = 0
    private var scrollBottom = rows - 1

    fun setChar(row: Int, col: Int, char: Char, attributes: CharacterAttributes) {
        if (row in 0 until rows && col in 0 until cols) {
            lines[row].setChar(col, TerminalChar(char, attributes))
        }
    }

    fun getChar(row: Int, col: Int): TerminalChar? = if (row in 0 until rows && col in 0 until cols) lines[row].getChar(col) else null
    fun getLine(row: Int): TerminalLine? = if (row in 0 until rows) lines[row] else null
    fun getLines(): List<TerminalLine> = lines.toList()

    fun clear() { lines.forEach { it.clear() } }

    fun clearLine(row: Int, startCol: Int, endCol: Int) {
        if (row !in 0 until rows) return
        for (col in startCol.coerceIn(0, cols - 1)..endCol.coerceIn(0, cols - 1)) {
            lines[row].setChar(col, TerminalChar(' ', CharacterAttributes()))
        }
    }

    fun scrollUp() {
        for (row in scrollTop until scrollBottom) {
            lines[row] = lines[row + 1]
        }
        lines[scrollBottom] = TerminalLine(cols)
    }

    fun setScrollRegion(top: Int, bottom: Int) {
        scrollTop = top.coerceIn(0, rows - 1)
        scrollBottom = bottom.coerceIn(scrollTop, rows - 1)
    }

    fun resize(newRows: Int, newCols: Int): TerminalBuffer {
        val newBuffer = TerminalBuffer(newRows, newCols)
        for (row in 0 until minOf(rows, newRows)) {
            for (col in 0 until minOf(cols, newCols)) {
                getChar(row, col)?.let { char ->
                    newBuffer.setChar(row, col, char.char, char.attributes)
                }
            }
        }
        return newBuffer
    }

    // Creating a shallow copy of the buffer to trigger StateFlow
    fun copy(): TerminalBuffer {
        val newBuffer = TerminalBuffer(rows, cols)
        for (i in 0 until rows) {
            newBuffer.lines[i] = this.lines[i]
        }
        newBuffer.scrollTop = this.scrollTop
        newBuffer.scrollBottom = this.scrollBottom
        return newBuffer
    }
}

class TerminalLine(private val cols: Int, val id: Long = nextLineId.getAndIncrement()) {
    private val chars: MutableList<TerminalChar> = MutableList(cols) { TerminalChar(' ', CharacterAttributes()) }
    var wrapped: Boolean = false

    fun setChar(col: Int, char: TerminalChar) { if (col in 0 until cols) chars[col] = char }
    fun getChar(col: Int): TerminalChar? = if (col in 0 until cols) chars[col] else null
    fun clear() {
        for (i in 0 until cols) chars[i] = TerminalChar(' ', CharacterAttributes())
        wrapped = false
    }
    fun copy(): TerminalLine {
        // 🔥 FIX: Pass the original ID to the new line to keep LazyColumn keys stable.
        val newLine = TerminalLine(cols, id)
        for (i in 0 until cols) {
            newLine.chars[i] = this.chars[i].copy()
        }
        newLine.wrapped = this.wrapped
        return newLine
    }
    fun getText(): String = chars.joinToString("") { it.char.toString() }
    fun getChars(): List<TerminalChar> = chars.toList()
    fun isBlank(): Boolean = chars.all { it.char == ' ' }
}

data class TerminalChar(val char: Char, val attributes: CharacterAttributes)