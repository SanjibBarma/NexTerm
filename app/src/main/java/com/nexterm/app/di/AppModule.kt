package com.nexterm.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.nexterm.app.data.local.database.NexTermDatabase
import com.nexterm.app.data.local.preferences.AppPreferences
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import kotlin.jvm.java

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nexterm_preferences")

val appModule = module {
    single { androidContext().dataStore }
    single { AppPreferences(get()) }
    single {
        Room.databaseBuilder(
            androidContext(),
            NexTermDatabase::class.java,
            "nexterm_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    single { get<NexTermDatabase>().sessionDao() }
    single { get<NexTermDatabase>().commandHistoryDao() }
}