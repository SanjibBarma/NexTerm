package com.nexterm.app.terminal.buffer

import com.nexterm.app.terminal.CharacterAttributes

class TerminalBuffer(
    private var rows: Int,
    private var cols: Int
) {
    private val lines: MutableList<TerminalLine> = MutableList(rows) { TerminalLine(cols) }
    private var scrollTop = 0
    private var scrollBottom = rows - 1

    fun setChar(row: Int, col: Int, char: Char, attributes: CharacterAttributes) {
        if (row in 0 until rows && col in 0 until cols) {
            lines[row].setChar(col, TerminalChar(char, attributes))
        }
    }

    fun getChar(row: Int, col: Int): TerminalChar? {
        return if (row in 0 until rows && col in 0 until cols) {
            lines[row].getChar(col)
        } else null
    }

    fun getLine(row: Int): TerminalLine? {
        return if (row in 0 until rows) lines[row] else null
    }

    fun getLines(): List<TerminalLine> = lines.toList()

    fun clear() {
        lines.forEach { it.clear() }
    }

    fun clearRange(startRow: Int, startCol: Int, endRow: Int, endCol: Int) {
        for (row in startRow..endRow.coerceAtMost(rows - 1)) {
            val start = if (row == startRow) startCol else 0
            val end = if (row == endRow) endCol else cols - 1
            for (col in start..end.coerceAtMost(cols - 1)) {
                lines[row].setChar(col, TerminalChar(' ', CharacterAttributes()))
            }
        }
    }

    fun clearLine(row: Int, startCol: Int, endCol: Int) {
        if (row in 0 until rows) {
            for (col in startCol..endCol.coerceAtMost(cols - 1)) {
                lines[row].setChar(col, TerminalChar(' ', CharacterAttributes()))
            }
        }
    }

    fun scrollUp(count: Int = 1) {
        repeat(count) {
            for (row in scrollTop until scrollBottom) {
                lines[row] = lines[row + 1]
            }
            lines[scrollBottom] = TerminalLine(cols)
        }
    }

    fun scrollDown(count: Int = 1) {
        repeat(count) {
            for (row in scrollBottom downTo scrollTop + 1) {
                lines[row] = lines[row - 1]
            }
            lines[scrollTop] = TerminalLine(cols)
        }
    }

    fun setScrollRegion(top: Int, bottom: Int) {
        scrollTop = top.coerceIn(0, rows - 1)
        scrollBottom = bottom.coerceIn(0, rows - 1)
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

    fun copy(): TerminalBuffer {
        val newBuffer = TerminalBuffer(rows, cols)
        lines.forEachIndexed { index, line ->
            newBuffer.lines[index] = line.copy()
        }
        newBuffer.scrollTop = scrollTop
        newBuffer.scrollBottom = scrollBottom
        return newBuffer
    }
}

class TerminalLine(private val cols: Int) {
    private val chars: MutableList<TerminalChar> = MutableList(cols) {
        TerminalChar(' ', CharacterAttributes())
    }

    var wrapped: Boolean = false

    fun setChar(col: Int, char: TerminalChar) {
        if (col in 0 until cols) {
            chars[col] = char
        }
    }

    fun getChar(col: Int): TerminalChar? {
        return if (col in 0 until cols) chars[col] else null
    }

    fun clear() {
        for (i in 0 until cols) {
            chars[i] = TerminalChar(' ', CharacterAttributes())
        }
        wrapped = false
    }

    fun copy(): TerminalLine {
        val newLine = TerminalLine(cols)
        chars.forEachIndexed { index, char ->
            newLine.chars[index] = char.copy()
        }
        newLine.wrapped = wrapped
        return newLine
    }

    fun getText(): String = chars.map { it.char }.joinToString("")

    fun getChars(): List<TerminalChar> = chars.toList()
}

data class TerminalChar(
    val char: Char,
    val attributes: CharacterAttributes
)