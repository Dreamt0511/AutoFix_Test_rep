package com.pocketagent.app

import android.app.Application
import android.util.Log
import com.pocketagent.app.agent.GitUpdater
import com.pocketagent.app.termux.TermuxBootstrap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PocketAgentApp : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this

        appScope.launch {
            bootstrap()
        }
    }

    private suspend fun bootstrap() {
        Log.i(TAG, "Bootstrapping Pocket-Agent...")

        // Step 1: Ensure Termux runtime is ready
        TermuxBootstrap.ensureReady(this)

        // Step 2: Clone/update agent from GitHub
        val updater = GitUpdater(this)
        updater.run()

        Log.i(TAG, "Bootstrap complete")
    }

    companion object {
        private const val TAG = "PocketAgentApp"

        lateinit var instance: PocketAgentApp
            private set
    }
}