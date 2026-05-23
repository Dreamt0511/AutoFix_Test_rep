package com.pocketagent.app.termux

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Bridge between Android and the Termux shell.
 *
 * Provides:
 * - Execute shell commands synchronously or asynchronously
 * - Stream output line-by-line via StateFlow
 * - Manage the persistent shell session for the agent
 */
class TermuxBridge {

    companion object {
        private const val TAG = "TermuxBridge"

        /** Shell binary to use */
        private const val SHELL = "/data/data/com.termux/files/usr/bin/bash"

        /** Fallback shell */
        private const val FALLBACK_SHELL = "/system/bin/sh"
    }

    private val _output = MutableStateFlow("")
    val output: StateFlow<String> = _output

    private var currentProcess: Process? = null

    /**
     * Execute a command in the Termux shell and return the full output.
     * Uses the Termux environment PATH and libraries.
     */
    suspend fun execute(command: String): CommandResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Executing: $command")

        val shell = if (TermuxBootstrap.isReady) TermuxBootstrap.termuxUsr + "/bin/bash"
                    else FALLBACK_SHELL

        try {
            val processBuilder = ProcessBuilder(shell, "-c", command)
                .directory(java.io.File(TermuxBootstrap.termuxRoot + "/home"))

            // Set Termux environment
            val env = processBuilder.environment()
            env["HOME"] = TermuxBootstrap.termuxRoot + "/home"
            env["PATH"] = TermuxBootstrap.termuxUsr + "/bin:" +
                          TermuxBootstrap.termuxUsr + "/bin/applets:" +
                          "/system/bin:/system/xbin"
            env["LD_LIBRARY_PATH"] = TermuxBootstrap.termuxUsr + "/lib"
            env["TMPDIR"] = TermuxBootstrap.termuxRoot + "/tmp"
            env["TERM"] = "xterm-256color"
            env["LANG"] = "en_US.UTF-8"

            val process = processBuilder.start()
            currentProcess = process

            val stdout = BufferedReader(InputStreamReader(process.inputStream))
            val stderr = BufferedReader(InputStreamReader(process.errorStream))

            val outputBuilder = StringBuilder()
            var line: String?

            // Read stdout
            while (stdout.readLine().also { line = it } != null) {
                outputBuilder.appendLine(line)
                _output.value = line ?: ""
            }

            // Read stderr
            while (stderr.readLine().also { line = it } != null) {
                outputBuilder.appendLine("[ERR] $line")
            }

            val exitCode = process.waitFor()
            currentProcess = null

            CommandResult(
                exitCode = exitCode,
                output = outputBuilder.toString().trim(),
                success = exitCode == 0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Command execution failed: ${e.message}")
            currentProcess = null
            CommandResult(
                exitCode = -1,
                output = "Error: ${e.message}",
                success = false
            )
        }
    }

    /**
     * Kill the currently running process if any.
     */
    fun kill() {
        currentProcess?.let {
            it.destroyForcibly()
            currentProcess = null
            Log.d(TAG, "Process killed")
        }
    }

    /**
     * Check if a command is currently running.
     */
    val isRunning: Boolean
        get() = currentProcess?.isAlive == true
}

data class CommandResult(
    val exitCode: Int,
    val output: String,
    val success: Boolean
)