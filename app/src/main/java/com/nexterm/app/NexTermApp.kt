package com.nexterm.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.nexterm.app.di.appModule
import com.nexterm.app.di.repositoryModule
import com.nexterm.app.di.viewModelModule
import com.nexterm.app.di.terminalModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class NexTermApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        initKoin()
        createNotificationChannels()
        initializeTerminalEnvironment()
    }

    private fun initKoin() {
        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@NexTermApp)
            modules(
                appModule,
                repositoryModule,
                viewModelModule,
                terminalModule
            )
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                TERMINAL_CHANNEL_ID,
                getString(R.string.terminal_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Terminal session notifications"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun initializeTerminalEnvironment() {
        val homeDir = filesDir
        val binDir = java.io.File(homeDir, "usr/bin")
        val etcDir = java.io.File(homeDir, "usr/etc")
        val tmpDir = java.io.File(homeDir, "tmp")
        val libDir = java.io.File(homeDir, "usr/lib")

        listOf(binDir, etcDir, tmpDir, libDir).forEach { dir ->
            if (!dir.exists()) dir.mkdirs()
        }
    }

    companion object {
        const val TERMINAL_CHANNEL_ID = "nexterm_terminal"
        lateinit var instance: NexTermApp
            private set
    }
}