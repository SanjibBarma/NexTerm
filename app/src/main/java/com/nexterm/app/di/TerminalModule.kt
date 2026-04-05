package com.nexterm.app.di

import com.nexterm.app.terminal.TerminalEmulator
import com.nexterm.app.terminal.TerminalSessionManager
import com.nexterm.app.terminal.ansi.AnsiParser
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val terminalModule = module {
    single { TerminalSessionManager(androidContext()) }
    factory { AnsiParser() }
    factory { params ->
        TerminalEmulator(
            rows = params.get<Int>(),
            cols = params.get<Int>(),
            ansiParser = get()
        )
    }
}