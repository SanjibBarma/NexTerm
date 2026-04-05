package com.nexterm.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexterm.app.data.local.preferences.TerminalSettings
import com.nexterm.app.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.nexterm.app.package_manager.PackageInfo
import com.nexterm.app.package_manager.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val packageManager: PackageManager
) : ViewModel() {

    private val _availablePackages = MutableStateFlow<List<PackageInfo>>(emptyList())
    val availablePackages = _availablePackages.asStateFlow()

    init {
        loadPackages()
    }

    private fun loadPackages() {
        _availablePackages.value = packageManager.listAvailable().values.toList()
    }

    fun isInstalled(packageName: String): Boolean {
        return packageManager.isInstalled(packageName)
    }

    val settings: StateFlow<TerminalSettings> = settingsRepository.terminalSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TerminalSettings()
        )

    fun updateFontSize(size: Int) {
        viewModelScope.launch {
            settingsRepository.updateFontSize(size)
        }
    }

    fun updateColorScheme(scheme: String) {
        viewModelScope.launch {
            settingsRepository.updateColorScheme(scheme)
        }
    }

    fun updateCursorBlink(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateCursorBlink(enabled)
        }
    }

    fun updateKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateKeepScreenOn(enabled)
        }
    }

    fun updateExtraKeysEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateExtraKeysEnabled(enabled)
        }
    }

    fun updateExtraKeysRow(keys: String) {
        viewModelScope.launch {
            settingsRepository.updateExtraKeysRow(keys)
        }
    }

    fun updateScrollbackLines(lines: Int) {
        viewModelScope.launch {
            settingsRepository.updateScrollbackLines(lines)
        }
    }
}