package com.pocketagent.app.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketagent.app.agent.AgentBridge
import com.pocketagent.app.agent.GitUpdater
import com.pocketagent.app.data.SettingsRepository
import kotlinx.coroutines.launch

data class Message(
    val id: String,
    val type: String, // "user", "agent", "status", "error"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class MainUiState(
    val messages: List<Message> = emptyList(),
    val isProcessing: Boolean = false,
    val agentStatus: String = "未连接",
    val lastUpdate: String? = null
)

class MainViewModel(
    private val agentBridge: AgentBridge,
    private val gitUpdater: GitUpdater,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    var uiState by mutableStateOf(MainUiState())
        private set

    init {
        // 启动时检查更新
        viewModelScope.launch {
            checkForUpdates()
            connectToAgent()
        }
    }

    private suspend fun checkForUpdates() {
        val result = gitUpdater.run()
        when (result.status) {
            GitUpdater.UpdateStatus.UPDATED -> {
                addStatusMessage("Agent 已更新到最新版本")
            }
            GitUpdater.UpdateStatus.SEED_FALLBACK -> {
                addStatusMessage("使用内置 Agent 代码")
            }
            GitUpdater.UpdateStatus.FAILED -> {
                addErrorMessage("Agent 代码更新失败")
            }
        }
    }

    private suspend fun connectToAgent() {
        val connected = agentBridge.start()
        uiState = uiState.copy(
            agentStatus = if (connected) "已连接" else "连接失败"
        )
        if (connected) {
            addStatusMessage("Agent 已连接")
        } else {
            addErrorMessage("Agent 连接失败，请检查设置")
        }
    }

    fun sendTask(task: String) {
        viewModelScope.launch {
            // 添加用户消息
            addUserMessage(task)

            // 设置处理状态
            uiState = uiState.copy(isProcessing = true)
            addStatusMessage("正在处理任务...")

            try {
                // 发送任务给 Agent
                val success = agentBridge.sendTask(task)
                if (success) {
                    addStatusMessage("任务已发送")
                } else {
                    addErrorMessage("任务发送失败")
                }
            } catch (e: Exception) {
                addErrorMessage("任务执行错误: ${e.message}")
            } finally {
                uiState = uiState.copy(isProcessing = false)
            }
        }
    }

    private fun addUserMessage(content: String) {
        val message = Message(
            id = "user_${System.currentTimeMillis()}",
            type = "user",
            content = content
        )
        uiState = uiState.copy(
            messages = uiState.messages + message
        )
    }

    private fun addAgentMessage(content: String) {
        val message = Message(
            id = "agent_${System.currentTimeMillis()}",
            type = "agent",
            content = content
        )
        uiState = uiState.copy(
            messages = uiState.messages + message
        )
    }

    private fun addStatusMessage(content: String) {
        val message = Message(
            id = "status_${System.currentTimeMillis()}",
            type = "status",
            content = content
        )
        uiState = uiState.copy(
            messages = uiState.messages + message
        )
    }

    private fun addErrorMessage(content: String) {
        val message = Message(
            id = "error_${System.currentTimeMillis()}",
            type = "error",
            content = content
        )
        uiState = uiState.copy(
            messages = uiState.messages + message
        )
    }

    fun clearHistory() {
        uiState = uiState.copy(messages = emptyList())
        addStatusMessage("历史记录已清空")
    }

    fun retryConnection() {
        viewModelScope.launch {
            connectToAgent()
        }
    }
}