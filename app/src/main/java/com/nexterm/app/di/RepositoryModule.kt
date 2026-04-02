package com.nexterm.app.di

import com.nexterm.app.data.repository.FileRepository
import com.nexterm.app.data.repository.FileRepositoryImpl
import com.nexterm.app.data.repository.SessionRepository
import com.nexterm.app.data.repository.SessionRepositoryImpl
import com.nexterm.app.data.repository.SettingsRepository
import com.nexterm.app.data.repository.SettingsRepositoryImpl
import org.koin.dsl.module

val repositoryModule = module {
    single<FileRepository> { FileRepositoryImpl(get()) }
    single<SessionRepository> { SessionRepositoryImpl(get(), get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
}