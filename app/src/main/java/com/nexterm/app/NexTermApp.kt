package com.nexterm.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.nexterm.app.di.appModule
import com.nexterm.app.di.repositoryModule
import com.nexterm.app.di.terminalModule
import com.nexterm.app.di.viewModelModule
import com.nexterm.app.package_manager.BootstrapManager
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.java.KoinJavaComponent
import java.io.File

class NexTermApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        initKoin()
        createNotificationChannels()
        initializeTerminalEnvironment()
        initializeBootstrap()
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
        val root = filesDir
        val dirs = listOf(
            File(root, "usr"),
            File(root, "usr/bin"),
            File(root, "usr/lib"),
            File(root, "usr/etc"),
            File(root, "usr/share"),
            File(root, "tmp"),
            File(root, "home"),
            File(root, "packages"),
            File(root, ".bootstrap")
        )

        dirs.forEach { dir ->
            if (!dir.exists()) dir.mkdirs()
        }
    }

    private fun initializeBootstrap() {
        val bootstrapManager: BootstrapManager =
            KoinJavaComponent.get(BootstrapManager::class.java)
        bootstrapManager.initialize()
    }

    companion object {
        const val TERMINAL_CHANNEL_ID = "nexterm_terminal"
        lateinit var instance: NexTermApp
            private set
    }
}