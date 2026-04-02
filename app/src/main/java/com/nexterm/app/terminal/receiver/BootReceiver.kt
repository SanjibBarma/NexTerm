package com.nexterm.app.terminal.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nexterm.app.terminal.service.TerminalService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Optionally start service on boot
            // val serviceIntent = Intent(context, TerminalService::class.java)
            // context.startForegroundService(serviceIntent)
        }
    }
}