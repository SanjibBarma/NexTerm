package com.nexterm.app.di

import com.nexterm.app.domain.usecase.CreateSessionUseCase
import com.nexterm.app.domain.usecase.GetActiveSessionsUseCase
import org.koin.dsl.module

val useCaseModule = module {
    factory { CreateSessionUseCase(get()) }
    factory { GetActiveSessionsUseCase(get()) }
}
