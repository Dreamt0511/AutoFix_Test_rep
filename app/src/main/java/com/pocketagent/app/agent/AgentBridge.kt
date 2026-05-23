package com.pocketagent.app.agent

import android.content.Context
import android.util.Log
import com.pocketagent.app.termux.TermuxBridge
import com.pocketagent.app.termux.TermuxBootstrap

/**
 * Bridge between the Android UI and the Python agent running in Termux.
 *
 * This is the bridge that:
 * - Starts the Python agent process
 * - Sends user tasks to the agent
 * - Receives step-by-step status updates from the agent
 *
 * Communication protocol:
 * - UI sends task description → AgentBridge writes to stdin of Python agent
 * - Python agent outputs JSON lines to stdout → AgentBridge parses and emits to UI
 *
 * JSON line format (from agent to UI):
 * {
 *   "type": "step",
 *   "status": "planning|executing|done|error",
 *   "message": "正在打开抖音...",
 *   "timestamp": 1234567890
 * }
 */
class AgentBridge(private val context: Context) {

    companion object {
        private const val TAG = "AgentBridge"
        private const val AGENT_DIR = "agent-seed"
        private const val ENTRY_POINT = "stable_entry.py"
    }

    private val termuxBridge = TermuxBridge()
    private var agentProcess: Process? = null
    private var isRunning = false

    /**
     * Start the Python agent process.
     * Returns true if the agent started successfully.
     */
    suspend fun start(): Boolean {
        if (isRunning) {
            Log.w(TAG, "Agent already running")
            return true
        }

        if (!TermuxBootstrap.isReady) {
            Log.e(TAG, "Termux environment not ready")
            return false
        }

        val agentPath = "${context.filesDir}/$AGENT_DIR"
        val pythonBin = "${TermuxBootstrap.termuxUsr}/bin/python3"

        Log.i(TAG, "Starting agent: $pythonBin $agentPath/$ENTRY_POINT")

        try {
            val result = termuxBridge.execute(
                "cd $agentPath && $pythonBin $ENTRY_POINT"
            )
            isRunning = result.success
            return result.success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start agent: ${e.message}")
            return false
        }
    }

    /**
     * Send a task to the running agent.
     */
    suspend fun sendTask(task: String): Boolean {
        if (!isRunning) {
            val started = start()
            if (!started) return false
        }

        Log.i(TAG, "Sending task: $task")

        // In MVP, we execute the agent with the task as an argument
        // Full implementation would use stdin/JSON protocol
        val agentPath = "${context.filesDir}/$AGENT_DIR"
        val pythonBin = "${TermuxBootstrap.termuxUsr}/bin/python3"
        val escapedTask = task.replace("\"", "\\\"")

        val result = termuxBridge.execute(
            "cd $agentPath && echo \"$escapedTask\" | $pythonBin $ENTRY_POINT --mode=task"
        )

        return result.success
    }

    /**
     * Stop the agent process.
     */
    fun stop() {
        termuxBridge.kill()
        isRunning = false
        Log.i(TAG, "Agent stopped")
    }
}