package com.nexterm.app.terminal.ansi

class AnsiParser {

    sealed class ParsedSequence {
        data class Text(val text: String) : ParsedSequence()
        data class ControlCode(val code: Char) : ParsedSequence()
        data class EscapeSequence(
            val type: String,
            val params: List<Int> = emptyList(),
            val intermediates: String = ""
        ) : ParsedSequence()
    }

    fun parse(input: String): List<ParsedSequence> {
        val sequences = mutableListOf<ParsedSequence>()
        var i = 0
        val textBuffer = StringBuilder()

        while (i < input.length) {
            val char = input[i]

            when {
                char == '\u001B' && i + 1 < input.length -> {
                    // Flush text buffer
                    if (textBuffer.isNotEmpty()) {
                        sequences.add(ParsedSequence.Text(textBuffer.toString()))
                        textBuffer.clear()
                    }

                    val result = parseEscapeSequence(input, i)
                    sequences.add(result.first)
                    i = result.second
                }
                char.isControl() -> {
                    if (textBuffer.isNotEmpty()) {
                        sequences.add(ParsedSequence.Text(textBuffer.toString()))
                        textBuffer.clear()
                    }
                    sequences.add(ParsedSequence.ControlCode(char))
                    i++
                }
                else -> {
                    textBuffer.append(char)
                    i++
                }
            }
        }

        if (textBuffer.isNotEmpty()) {
            sequences.add(ParsedSequence.Text(textBuffer.toString()))
        }

        return sequences
    }

    private fun parseEscapeSequence(input: String, startIndex: Int): Pair<ParsedSequence, Int> {
        var i = startIndex + 1

        if (i >= input.length) {
            return ParsedSequence.Text("\u001B") to i
        }

        val nextChar = input[i]

        return when (nextChar) {
            '[' -> parseCSI(input, i + 1)
            ']' -> parseOSC(input, i + 1)
            '(' -> parseCharset(input, i + 1)
            ')' -> parseCharset(input, i + 1)
            '#' -> parseScreenAlign(input, i + 1)
            '7' -> ParsedSequence.EscapeSequence("s") to i + 1
            '8' -> ParsedSequence.EscapeSequence("u") to i + 1
            'D' -> ParsedSequence.EscapeSequence("IND") to i + 1
            'E' -> ParsedSequence.EscapeSequence("NEL") to i + 1
            'H' -> ParsedSequence.EscapeSequence("HTS") to i + 1
            'M' -> ParsedSequence.EscapeSequence("RI") to i + 1
            'c' -> ParsedSequence.EscapeSequence("RIS") to i + 1
            else -> ParsedSequence.Text("\u001B$nextChar") to i + 1
        }
    }

    private fun parseCSI(input: String, startIndex: Int): Pair<ParsedSequence, Int> {
        var i = startIndex
        val params = mutableListOf<Int>()
        var currentParam = StringBuilder()
        val intermediates = StringBuilder()

        // Parse private mode indicator
        var privateMode = ""
        if (i < input.length && (input[i] == '?' || input[i] == '>' || input[i] == '!')) {
            privateMode = input[i].toString()
            i++
        }

        // Parse parameters
        while (i < input.length) {
            val char = input[i]
            when {
                char.isDigit() -> {
                    currentParam.append(char)
                    i++
                }
                char == ';' -> {
                    params.add(currentParam.toString().toIntOrNull() ?: 0)
                    currentParam = StringBuilder()
                    i++
                }
                char == ':' -> {
                    // Colon-separated subparameters
                    params.add(currentParam.toString().toIntOrNull() ?: 0)
                    currentParam = StringBuilder()
                    i++
                }
                char in ' '..'/' -> {
                    intermediates.append(char)
                    i++
                }
                char in '@'..'~' -> {
                    if (currentParam.isNotEmpty()) {
                        params.add(currentParam.toString().toIntOrNull() ?: 0)
                    }
                    return ParsedSequence.EscapeSequence(
                        type = privateMode + char.toString(),
                        params = params,
                        intermediates = intermediates.toString()
                    ) to i + 1
                }
                else -> break
            }
        }

        return ParsedSequence.Text("\u001B[") to i
    }

    private fun parseOSC(input: String, startIndex: Int): Pair<ParsedSequence, Int> {
        var i = startIndex
        val content = StringBuilder()

        while (i < input.length) {
            val char = input[i]
            when {
                char == '\u0007' -> {
                    return ParsedSequence.EscapeSequence(
                        type = "OSC",
                        params = listOf(content.toString().substringBefore(';').toIntOrNull() ?: 0)
                    ) to i + 1
                }
                char == '\u001B' && i + 1 < input.length && input[i + 1] == '\\' -> {
                    return ParsedSequence.EscapeSequence(
                        type = "OSC",
                        params = listOf(content.toString().substringBefore(';').toIntOrNull() ?: 0)
                    ) to i + 2
                }
                else -> {
                    content.append(char)
                    i++
                }
            }
        }

        return ParsedSequence.Text("\u001B]$content") to i
    }

    private fun parseCharset(input: String, startIndex: Int): Pair<ParsedSequence, Int> {
        if (startIndex < input.length) {
            return ParsedSequence.EscapeSequence(
                type = "charset",
                params = listOf(input[startIndex].code)
            ) to startIndex + 1
        }
        return ParsedSequence.Text("\u001B(") to startIndex
    }

    private fun parseScreenAlign(input: String, startIndex: Int): Pair<ParsedSequence, Int> {
        if (startIndex < input.length && input[startIndex] == '8') {
            return ParsedSequence.EscapeSequence(type = "DECALN") to startIndex + 1
        }
        return ParsedSequence.Text("\u001B#") to startIndex
    }

    private fun Char.isControl(): Boolean {
        return this in '\u0000'..'\u001F' || this == '\u007F'
    }
}