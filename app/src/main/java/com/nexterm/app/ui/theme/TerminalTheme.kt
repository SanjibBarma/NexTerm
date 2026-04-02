package com.nexterm.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class TerminalColorScheme(
    val key: String,
    val name: String,
    val background: Color,
    val foreground: Color,
    val cursor: Color,
    val selection: Color,
    val black: Color,
    val red: Color,
    val green: Color,
    val yellow: Color,
    val blue: Color,
    val magenta: Color,
    val cyan: Color,
    val white: Color,
    val brightBlack: Color,
    val brightRed: Color,
    val brightGreen: Color,
    val brightYellow: Color,
    val brightBlue: Color,
    val brightMagenta: Color,
    val brightCyan: Color,
    val brightWhite: Color
) {
    fun getColor(index: Int): Color {
        return when (index) {
            0 -> black
            1 -> red
            2 -> green
            3 -> yellow
            4 -> blue
            5 -> magenta
            6 -> cyan
            7 -> white
            8 -> brightBlack
            9 -> brightRed
            10 -> brightGreen
            11 -> brightYellow
            12 -> brightBlue
            13 -> brightMagenta
            14 -> brightCyan
            15 -> brightWhite
            else -> foreground
        }
    }
}

object TerminalThemes {
    val Monokai = TerminalColorScheme(
        key = "monokai",
        name = "Monokai",
        background = TerminalColors.MonokaiBackground,
        foreground = TerminalColors.MonokaiForeground,
        cursor = TerminalColors.MonokaiForeground,
        selection = Color(0x50F8F8F2),
        black = TerminalColors.MonokaiBlack,
        red = TerminalColors.MonokaiRed,
        green = TerminalColors.MonokaiGreen,
        yellow = TerminalColors.MonokaiYellow,
        blue = TerminalColors.MonokaiBlue,
        magenta = TerminalColors.MonokaiMagenta,
        cyan = TerminalColors.MonokaiCyan,
        white = TerminalColors.MonokaiWhite,
        brightBlack = Color(0xFF75715E),
        brightRed = Color(0xFFF92672),
        brightGreen = Color(0xFFA6E22E),
        brightYellow = Color(0xFFF4BF75),
        brightBlue = Color(0xFF66D9EF),
        brightMagenta = Color(0xFFAE81FF),
        brightCyan = Color(0xFFA1EFE4),
        brightWhite = Color(0xFFF9F8F5)
    )

    val Dracula = TerminalColorScheme(
        key = "dracula",
        name = "Dracula",
        background = TerminalColors.DraculaBackground,
        foreground = TerminalColors.DraculaForeground,
        cursor = TerminalColors.DraculaForeground,
        selection = Color(0x5044475A),
        black = TerminalColors.DraculaBlack,
        red = TerminalColors.DraculaRed,
        green = TerminalColors.DraculaGreen,
        yellow = TerminalColors.DraculaYellow,
        blue = TerminalColors.DraculaBlue,
        magenta = TerminalColors.DraculaMagenta,
        cyan = TerminalColors.DraculaCyan,
        white = TerminalColors.DraculaWhite,
        brightBlack = Color(0xFF6272A4),
        brightRed = Color(0xFFFF6E6E),
        brightGreen = Color(0xFF69FF94),
        brightYellow = Color(0xFFFFFFA5),
        brightBlue = Color(0xFFD6ACFF),
        brightMagenta = Color(0xFFFF92DF),
        brightCyan = Color(0xFFA4FFFF),
        brightWhite = Color(0xFFFFFFFF)
    )

    val Nord = TerminalColorScheme(
        key = "nord",
        name = "Nord",
        background = TerminalColors.NordBackground,
        foreground = TerminalColors.NordForeground,
        cursor = TerminalColors.NordForeground,
        selection = Color(0x504C566A),
        black = TerminalColors.NordBlack,
        red = TerminalColors.NordRed,
        green = TerminalColors.NordGreen,
        yellow = TerminalColors.NordYellow,
        blue = TerminalColors.NordBlue,
        magenta = TerminalColors.NordMagenta,
        cyan = TerminalColors.NordCyan,
        white = TerminalColors.NordWhite,
        brightBlack = Color(0xFF4C566A),
        brightRed = Color(0xFFBF616A),
        brightGreen = Color(0xFFA3BE8C),
        brightYellow = Color(0xFFEBCB8B),
        brightBlue = Color(0xFF81A1C1),
        brightMagenta = Color(0xFFB48EAD),
        brightCyan = Color(0xFF8FBCBB),
        brightWhite = Color(0xFFECEFF4)
    )

    val Solarized = TerminalColorScheme(
        key = "solarized",
        name = "Solarized Dark",
        background = TerminalColors.SolarizedBackground,
        foreground = TerminalColors.SolarizedForeground,
        cursor = TerminalColors.SolarizedForeground,
        selection = Color(0x50657B83),
        black = TerminalColors.SolarizedBlack,
        red = TerminalColors.SolarizedRed,
        green = TerminalColors.SolarizedGreen,
        yellow = TerminalColors.SolarizedYellow,
        blue = TerminalColors.SolarizedBlue,
        magenta = TerminalColors.SolarizedMagenta,
        cyan = TerminalColors.SolarizedCyan,
        white = TerminalColors.SolarizedWhite,
        brightBlack = Color(0xFF002B36),
        brightRed = Color(0xFFCB4B16),
        brightGreen = Color(0xFF586E75),
        brightYellow = Color(0xFF657B83),
        brightBlue = Color(0xFF839496),
        brightMagenta = Color(0xFF6C71C4),
        brightCyan = Color(0xFF93A1A1),
        brightWhite = Color(0xFFFDF6E3)
    )

    val Material = TerminalColorScheme(
        key = "material",
        name = "Material",
        background = TerminalColors.MaterialBackground,
        foreground = TerminalColors.MaterialForeground,
        cursor = TerminalColors.MaterialForeground,
        selection = Color(0x50FFFFFF),
        black = TerminalColors.MaterialBlack,
        red = TerminalColors.MaterialRed,
        green = TerminalColors.MaterialGreen,
        yellow = TerminalColors.MaterialYellow,
        blue = TerminalColors.MaterialBlue,
        magenta = TerminalColors.MaterialMagenta,
        cyan = TerminalColors.MaterialCyan,
        white = TerminalColors.MaterialWhite,
        brightBlack = Color(0xFF545454),
        brightRed = Color(0xFFFF8A80),
        brightGreen = Color(0xFF64FFDA),
        brightYellow = Color(0xFFFFFF00),
        brightBlue = Color(0xFFD4BBFF),
        brightMagenta = Color(0xFFFF80AB),
        brightCyan = Color(0xFF84FFFF),
        brightWhite = Color(0xFFFFFFFF)
    )

    val all = listOf(Monokai, Dracula, Nord, Solarized, Material)

    fun getByName(name: String): TerminalColorScheme {
        val normalized = name.trim().lowercase()

        return all.find { it.key == normalized }
            ?: all.find { it.name.lowercase() == normalized }
            ?: all.find { normalized.contains(it.key) }
            ?: all.find { it.name.lowercase().contains(normalized) }
            ?: Monokai
    }
}

val LocalTerminalColorScheme = staticCompositionLocalOf { TerminalThemes.Monokai }