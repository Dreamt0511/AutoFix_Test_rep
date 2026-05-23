package com.pocketagent.app.termux

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

/**
 * Foreground service that keeps the Termux shell process alive.
 * Required for long-running agent tasks that need a persistent shell.
 */
class TermuxService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Termux service started")
        // Keep the shell alive; actual process management is in TermuxBridge
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "Termux service destroyed")
        super.onDestroy()
    }

    companion object {
        private const val TAG = "TermuxService"
    }
}