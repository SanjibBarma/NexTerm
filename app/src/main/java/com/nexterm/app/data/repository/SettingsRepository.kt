package com.nexterm.app.data.repository

import com.nexterm.app.data.local.preferences.AppPreferences
import com.nexterm.app.data.local.preferences.TerminalSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val terminalSettings: Flow<TerminalSettings>
    suspend fun updateFontSize(size: Int)
    suspend fun updateColorScheme(scheme: String)
    suspend fun updateCursorBlink(enabled: Boolean)
    suspend fun updateKeepScreenOn(enabled: Boolean)
    suspend fun updateExtraKeysEnabled(enabled: Boolean)
    suspend fun updateExtraKeysRow(keys: String)
    suspend fun updateScrollbackLines(lines: Int)
    suspend fun updateTerminalSize(columns: Int, rows: Int)
}

class SettingsRepositoryImpl(
    private val appPreferences: AppPreferences
) : SettingsRepository {

    override val terminalSettings: Flow<TerminalSettings> = appPreferences.terminalSettings

    override suspend fun updateFontSize(size: Int) {
        appPreferences.updateFontSize(size)
    }

    override suspend fun updateColorScheme(scheme: String) {
        appPreferences.updateColorScheme(scheme)
    }

    override suspend fun updateCursorBlink(enabled: Boolean) {
        appPreferences.updateCursorBlink(enabled)
    }

    override suspend fun updateKeepScreenOn(enabled: Boolean) {
        appPreferences.updateKeepScreenOn(enabled)
    }

    override suspend fun updateExtraKeysEnabled(enabled: Boolean) {
        appPreferences.updateExtraKeysEnabled(enabled)
    }

    override suspend fun updateExtraKeysRow(keys: String) {
        appPreferences.updateExtraKeysRow(keys)
    }

    override suspend fun updateScrollbackLines(lines: Int) {
        appPreferences.updateScrollbackLines(lines)
    }

    override suspend fun updateTerminalSize(columns: Int, rows: Int) {
        appPreferences.updateTerminalSize(columns, rows)
    }
}