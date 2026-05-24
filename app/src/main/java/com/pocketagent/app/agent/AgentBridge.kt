package com.pocketagent.app.agent

import android.content.Context
import android.util.Log
import com.pocketagent.app.termux.TermuxBridge
import com.pocketagent.app.termux.TermuxBootstrap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class AgentBridge(private val context: Context) {

    companion object {
        private const val TAG = "AgentBridge"
        private const val AGENT_DIR = "agent-seed"
        private const val ENTRY_POINT = "stable_entry.py"
    }

    private val termuxBridge = TermuxBridge()

    suspend fun start(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!TermuxBootstrap.isReady) {
                    Log.e(TAG, "Termux environment not ready")
                    return@withContext false
                }

                val agentPath = "${context.filesDir}/$AGENT_DIR"
                val pythonBin = "${TermuxBootstrap.termuxUsr}/bin/python3"

                // 测试 agent 是否就绪
                val result = termuxBridge.execute(
                    "cd $agentPath && $pythonBin $ENTRY_POINT"
                )

                if (result.success) {
                    val output = result.output.trim()
                    if (output.isNotEmpty()) {
                        try {
                            val json = JSONObject(output)
                            if (json.getString("type") == "ready") {
                                Log.i(TAG, "Agent ready: ${json.toString()}")
                                return@withContext true
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Agent output not JSON: $output")
                        }
                    }
                }

                Log.e(TAG, "Agent start failed: ${result.output}")
                false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start agent: ${e.message}")
                false
            }
        }
    }

    suspend fun sendTask(task: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val agentPath = "${context.filesDir}/$AGENT_DIR"
                val pythonBin = "${TermuxBootstrap.termuxUsr}/bin/python3"

                // 转义任务中的特殊字符
                val escapedTask = task.replace("\"", "\\\"")
                val jsonTask = JSONObject(mapOf("task" to task)).toString()
                    .replace("\"", "\\\"")

                val command = """
                    cd $agentPath && 
                    echo '$jsonTask' | 
                    $pythonBin $ENTRY_POINT --mode=task
                """.trimIndent()

                Log.d(TAG, "Executing task: $task")
                val result = termuxBridge.execute(command)

                if (result.success) {
                    // 解析输出，逐行处理 JSON
                    val lines = result.output.lines()
                    for (line in lines) {
                        if (line.trim().isNotEmpty()) {
                            try {
                                val json = JSONObject(line)
                                val type = json.getString("type")
                                val status = json.optString("status", "")
                                val message = json.optString("message", "")

                                when (type) {
                                    "step" -> {
                                        Log.i(TAG, "Agent step [$status]: $message")
                                        // 这里可以发送到 UI 更新状态
                                    }
                                    "result" -> {
                                        val content = json.optString("content", "")
                                        Log.i(TAG, "Agent result: $content")
                                    }
                                    "error" -> {
                                        Log.e(TAG, "Agent error: $message")
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to parse agent output: $line")
                            }
                        }
                    }
                    true
                } else {
                    Log.e(TAG, "Task execution failed: ${result.output}")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send task: ${e.message}")
                false
            }
        }
    }

    suspend fun updateConfig(config: Map<String, String>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val agentPath = "${context.filesDir}/$AGENT_DIR"
                val pythonBin = "${TermuxBootstrap.termuxUsr}/bin/python3"

                val jsonConfig = JSONObject(config).toString()
                    .replace("\"", "\\\"")

                val command = """
                    cd $agentPath && 
                    echo '$jsonConfig' | 
                    $pythonBin $ENTRY_POINT --mode=config
                """.trimIndent()

                val result = termuxBridge.execute(command)
                if (result.success) {
                    try {
                        val json = JSONObject(result.output.trim())
                        return@withContext json.optBoolean("success", false)
                    } catch (e: Exception) {
                        Log.w(TAG, "Config update response not JSON: ${result.output}")
                    }
                }
                false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update config: ${e.message}")
                false
            }
        }
    }

    suspend fun getCurrentConfig(): Map<String, String> {
        return withContext(Dispatchers.IO) {
            try {
                val agentPath = "${context.filesDir}/$AGENT_DIR"
                val pythonBin = "${TermuxBootstrap.termuxUsr}/bin/python3"

                val command = """
                    cd $agentPath && 
                    $pythonBin $ENTRY_POINT --mode=get_config
                """.trimIndent()

                val result = termuxBridge.execute(command)
                if (result.success) {
                    try {
                        val json = JSONObject(result.output.trim())
                        if (json.getString("type") == "config") {
                            val configJson = json.getJSONObject("config")
                            val config = mutableMapOf<String, String>()
                            val keys = configJson.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                config[key] = configJson.getString(key)
                            }
                            return@withContext config
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse config: ${e.message}")
                    }
                }
                emptyMap()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get config: ${e.message}")
                emptyMap()
            }
        }
    }

    fun stop() {
        termuxBridge.kill()
        Log.i(TAG, "Agent stopped")
    }
}