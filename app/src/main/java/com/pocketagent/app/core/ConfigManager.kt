package com.pocketagent.app.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 配置管理器 — 持久化 .env 配置
 *
 * 基于主项目 .env.example 格式，支持:
 *   KEY=VALUE
 *   # 注释
 *   KEY="value with spaces"
 */
object ConfigManager {
    private const val TAG = "ConfigManager"
    private const val CONFIG_FILE = ".env"

    private var context: Context? = null

    fun init(ctx: Context) {
        context = ctx.applicationContext
    }

    /**
     * 读取所有配置
     */
    suspend fun loadAll(): Map<String, String> = withContext(Dispatchers.IO) {
        val configFile = getConfigFile() ?: return@withContext emptyMap()
        if (!configFile.exists()) return@withContext loadDefaults()

        val config = mutableMapOf<String, String>()
        configFile.readLines().forEach { line ->
            parseLine(line)?.let { (key, value) ->
                config[key] = value
            }
        }
        config
    }

    /**
     * 保存配置
     */
    suspend fun saveAll(config: Map<String, String>) = withContext(Dispatchers.IO) {
        val configFile = getConfigFile() ?: return@withContext
        val content = buildString {
            config.forEach { (key, value) ->
                append("$key=$value\n")
            }
        }
        configFile.writeText(content)
        Log.i(TAG, "Config saved to ${configFile.absolutePath}")
    }

    /**
     * 获取单个配置值
     */
    suspend fun get(key: String, defaultValue: String = ""): String {
        val all = loadAll()
        return all[key] ?: defaultValue
    }

    /**
     * 设置单个配置值
     */
    suspend fun set(key: String, value: String) {
        val all = loadAll().toMutableMap()
        all[key] = value
        saveAll(all)
    }

    /**
     * 删除配置
     */
    suspend fun remove(key: String) {
        val all = loadAll().toMutableMap()
        all.remove(key)
        saveAll(all)
    }

    /**
     * 重置为默认配置
     */
    suspend fun resetToDefaults() {
        saveAll(loadDefaults())
    }

    // ─── 私有方法 ───────────────────────────────

    private fun getConfigFile(): File? {
        val ctx = context ?: return null
        return File(ctx.filesDir, CONFIG_FILE)
    }

    private fun parseLine(line: String): Pair<String, String>? {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) return null

        val equalsIndex = trimmed.indexOf('=')
        if (equalsIndex < 0) return null

        val key = trimmed.substring(0, equalsIndex).trim()
        var value = trimmed.substring(equalsIndex + 1).trim()

        // 处理引号
        if (value.startsWith('"') && value.endsWith('"')) {
            value = value.substring(1, value.length - 1)
        } else if (value.startsWith('\'') && value.endsWith('\'')) {
            value = value.substring(1, value.length - 1)
        }

        return key to value
    }

    private fun loadDefaults(): Map<String, String> {
        return mapOf(
            // MCP 服务器
            "MCP_SERVER_URL" to "http://127.0.0.1:7474/mcp",

            // 主 Agent 模型配置 (云端)
            "DEFAULT_LLM_BASE_URL" to "https://api.longcat.chat/openai/v1",
            "LLM_API_KEY" to "",
            "LLM_MODEL" to "longcat-flash-lite",
            "LLM_TEMPERATURE" to "0.7",
            "LLM_MAX_TOKENS" to "8000",

            // 主 Agent 模型配置 (本地)
            "DEFAULT_LLM_BASE_URL_LOCAL" to "http://127.0.0.1:8080/v1",
            "LLM_API_KEY_LOCAL" to "dummy",
            "LLM_MODEL_LOCAL" to "gelab-zero-4b-preview",

            // 子 Agent 模型配置
            "EXECUTOR_LLM_BASE_URL" to "",
            "EXECUTOR_API_KEY" to "",
            "EXECUTOR_MODEL" to "qwen2.5:7b",
            "EXECUTOR_TEMPERATURE" to "0.3",
            "EXECUTOR_MAX_TOKENS" to "8192",

            // Ollama
            "OLLAMA_BASE_URL" to "http://localhost:11434",

            // Termux API
            "TERMUX_API_ENABLED" to "true",
            "TERMUX_API_CHECK_CMD" to "which termux-battery-status",

            // 环境感知
            "ENV_LIGHT_SENSOR_CMD" to "termux-sensor -s \"tcs3760 Ambient Light Sensor Non-wakeup\" -n 1",
            "ENV_ACCEL_SENSOR_CMD" to "termux-sensor -s \"lsm6dsv Accelerometer Non-wakeup\" -n 3"
        )
    }
}