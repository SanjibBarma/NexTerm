package com.nexterm.app.data.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class AppPreferences(private val dataStore: DataStore<Preferences>) {

    val terminalSettings: Flow<TerminalSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            TerminalSettings(
                fontSize = preferences[FONT_SIZE] ?: 14,
                fontFamily = preferences[FONT_FAMILY] ?: "monospace",
                colorScheme = preferences[COLOR_SCHEME]?.trim()?.lowercase() ?: "monokai",
                cursorStyle = preferences[CURSOR_STYLE] ?: "block",
                cursorBlink = preferences[CURSOR_BLINK] ?: true,
                bellEnabled = preferences[BELL_ENABLED] ?: true,
                vibrateOnBell = preferences[VIBRATE_ON_BELL] ?: true,
                keepScreenOn = preferences[KEEP_SCREEN_ON] ?: false,
                extraKeysEnabled = preferences[EXTRA_KEYS_ENABLED] ?: true,
                extraKeysRow = preferences[EXTRA_KEYS_ROW] ?: DEFAULT_EXTRA_KEYS,
                scrollbackLines = preferences[SCROLLBACK_LINES] ?: 10000,
                terminalColumns = preferences[TERMINAL_COLUMNS] ?: 0,
                terminalRows = preferences[TERMINAL_ROWS] ?: 0
            )
        }

    suspend fun updateFontSize(size: Int) {
        dataStore.edit { preferences ->
            preferences[FONT_SIZE] = size
        }
    }

    suspend fun updateColorScheme(scheme: String) {
        dataStore.edit { preferences ->
            preferences[COLOR_SCHEME] = scheme.trim().lowercase()
        }
    }

    suspend fun updateCursorBlink(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[CURSOR_BLINK] = enabled
        }
    }

    suspend fun updateKeepScreenOn(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEEP_SCREEN_ON] = enabled
        }
    }

    suspend fun updateExtraKeysEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[EXTRA_KEYS_ENABLED] = enabled
        }
    }

    suspend fun updateExtraKeysRow(keys: String) {
        dataStore.edit { preferences ->
            preferences[EXTRA_KEYS_ROW] = keys
        }
    }

    suspend fun updateScrollbackLines(lines: Int) {
        dataStore.edit { preferences ->
            preferences[SCROLLBACK_LINES] = lines
        }
    }

    suspend fun updateTerminalSize(columns: Int, rows: Int) {
        dataStore.edit { preferences ->
            preferences[TERMINAL_COLUMNS] = columns
            preferences[TERMINAL_ROWS] = rows
        }
    }

    companion object {
        private val FONT_SIZE = intPreferencesKey("font_size")
        private val FONT_FAMILY = stringPreferencesKey("font_family")
        private val COLOR_SCHEME = stringPreferencesKey("color_scheme")
        private val CURSOR_STYLE = stringPreferencesKey("cursor_style")
        private val CURSOR_BLINK = booleanPreferencesKey("cursor_blink")
        private val BELL_ENABLED = booleanPreferencesKey("bell_enabled")
        private val VIBRATE_ON_BELL = booleanPreferencesKey("vibrate_on_bell")
        private val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        private val EXTRA_KEYS_ENABLED = booleanPreferencesKey("extra_keys_enabled")
        private val EXTRA_KEYS_ROW = stringPreferencesKey("extra_keys_row")
        private val SCROLLBACK_LINES = intPreferencesKey("scrollback_lines")
        private val TERMINAL_COLUMNS = intPreferencesKey("terminal_columns")
        private val TERMINAL_ROWS = intPreferencesKey("terminal_rows")

        const val DEFAULT_EXTRA_KEYS = "ESC|TAB|CTRL|ALT|HOME|UP|END|PGUP|LEFT|DOWN|RIGHT|PGDN"
    }
}

data class TerminalSettings(
    val fontSize: Int = 14,
    val fontFamily: String = "monospace",
    val colorScheme: String = "monokai",
    val cursorStyle: String = "block",
    val cursorBlink: Boolean = true,
    val bellEnabled: Boolean = true,
    val vibrateOnBell: Boolean = true,
    val keepScreenOn: Boolean = false,
    val extraKeysEnabled: Boolean = true,
    val extraKeysRow: String = AppPreferences.DEFAULT_EXTRA_KEYS,
    val scrollbackLines: Int = 10000,
    val terminalColumns: Int = 0,
    val terminalRows: Int = 0
)