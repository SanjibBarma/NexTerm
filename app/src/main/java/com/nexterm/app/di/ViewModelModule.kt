package com.nexterm.app.di

import com.nexterm.app.ui.screens.files.FilesViewModel
import com.nexterm.app.ui.screens.settings.SettingsViewModel
import com.nexterm.app.ui.screens.terminal.TerminalViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { TerminalViewModel(get(), get(), get(), get()) }
    viewModel { FilesViewModel(get()) }
    viewModel { SettingsViewModel(get(), get()) }
}