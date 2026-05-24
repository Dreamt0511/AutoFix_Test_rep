package com.pocketagent.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketagent.app.bridge.AgentDaemon
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DiagnosticViewModel : ViewModel() {

    val report: StateFlow<AgentDaemon.DiagnosticReport>
        get() = AgentDaemon.diagnostics

    val daemonState: StateFlow<AgentDaemon.DaemonState>
        get() = AgentDaemon.daemonState

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _connectedText = MutableStateFlow("未连接")
    val connectedText: StateFlow<String> = _connectedText

    init {
        updateConnectionText()
    }

    private fun updateConnectionText() {
        viewModelScope.launch {
            daemonState.collect { state ->
                _connectedText.value = when (state) {
                    AgentDaemon.DaemonState.RUNNING -> "守护进程运行中"
                    AgentDaemon.DaemonState.STARTING -> "正在启动..."
                    AgentDaemon.DaemonState.ERROR -> "守护进程异常"
                    AgentDaemon.DaemonState.STOPPED -> "守护进程已停止"
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(500)
            _isRefreshing.value = false
        }
    }
}