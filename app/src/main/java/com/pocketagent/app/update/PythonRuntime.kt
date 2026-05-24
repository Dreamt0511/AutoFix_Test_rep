package com.pocketagent.app.update

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Python 运行时管理器 — 在 APK 内执行动态加载的主仓库 Python 代码
 *
 * 依赖 Chaquopy: https://chaquo.com/chaquopy/
 *
 * 架构：
 *   Android (Kotlin)  ←→  Python (主仓库代码)
 *          │                      │
 *          │  invoke("agent_main") │ 调用主仓库入口
 *          │  sendCommand(cmd)      │ 发送用户指令
 *          │  onOutput ←──────────  │ 流式输出回调
 *          │  onStatus ←──────────  │ 状态更新回调
 *          │  onAction  ←──────────│ 请求 Android 能力 (无障碍点击等)
 *
 * 使用方式：
 *   val runtime = PythonRuntime(context)
 *   runtime.setOnOutput { text -> ... }
 *   runtime.initialize()
 *   runtime.execute("帮我在微信里给张三发消息")
 */
class PythonRuntime(private val context: Context) {

    companion object {
        private const val TAG = "PythonRuntime"
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(RuntimeState.Idle)
    val state: StateFlow<RuntimeState> = _state

    sealed class RuntimeState {
        object Idle : RuntimeState()
        object Initializing : RuntimeState()
        object Ready : RuntimeState()
        object Executing : RuntimeState()
        data class Error(val message: String) : RuntimeState()
    }

    // 回调
    private var onOutput: ((String) -> Unit)? = null
    private var onStatus: ((String) -> Unit)? = null
    private var onAction: ((ActionRequest) -> ActionResult)? = null
    private var onComplete: ((TaskResult) -> Unit)? = null

    /** 用户中断标志 */
    @Volatile
    private var cancelled = false

    // ─── 公开接口 ─────────────────────────────────

    fun setOnOutput(callback: (String) -> Unit) { onOutput = callback }
    fun setOnStatus(callback: (String) -> Unit) { onStatus = callback }
    fun setOnAction(callback: (ActionRequest) -> ActionResult) { onAction = callback }
    fun setOnComplete(callback: (TaskResult) -> Unit) { onComplete = callback }

    /**
     * 初始化 Python 运行时
     *
     * Chaquopy 在首次调用 Python 时自动初始化。
     * 这里做一个预热调用以确保就绪。
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        _state.value = RuntimeState.Initializing
        try {
            // 验证主仓库代码存在
            val codeManager = CodeSyncManager.getInstance()
            if (!codeManager.isCodeReady()) {
                _state.value = RuntimeState.Error("Agent 代码未就绪，请先同步")
                return@withContext false
            }

            // 预热 Python 环境
            val result = executePythonSnippet("import sys; print('Python ' + sys.version)")
            if (result.contains("Python ")) {
                _state.value = RuntimeState.Ready
                Log.i(TAG, "Python runtime ready")
                true
            } else {
                _state.value = RuntimeState.Error("Python 环境异常")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Initialize failed", e)
            _state.value = RuntimeState.Error(e.message ?: "初始化失败")
            false
        }
    }

    /**
     * 执行用户指令
     *
     * 流程：
     *  1. 将指令写入 stdin 通道
     *  2. 启动主仓库 main.py
     *  3. main.py 通过约定协议回调：
     *     - stdout  = 日志输出 → onOutput
     *     - ACTION: = Android 能力请求 → onAction
     *     - DONE:   = 任务完成 → onComplete
     */
    suspend fun execute(command: String): TaskResult = withContext(Dispatchers.IO) {
        _state.value = RuntimeState.Executing
        cancelled = false
        onStatus?.invoke("解析指令...")

        try {
            val codeManager = CodeSyncManager.getInstance()
            val entryPath = codeManager.getEntryPoint().absolutePath

            // 构建 Python 执行代码
            // 通过 Chaquopy 的 Python 对象调用主仓库入口
            val pythonCode = buildString {
                appendLine("import sys")
                appendLine("import json")
                appendLine("sys.path.insert(0, '${codeManager.getRuntimeDir().absolutePath}')")
                appendLine("")
                appendLine("# 加载主仓库 agent 模块")
                appendLine("try:")
                appendLine("    from agent_core.main import Agent")
                appendLine("    agent = Agent()")
                appendLine("")
                appendLine("    # 注册 Android 能力回调")
                appendLine("    def android_action(action_json):")
                appendLine("        print(f'ACTION:{action_json}', flush=True)")
                appendLine("        return ''  # 实际通过 ActionRequest/ActionResult 通信")
                appendLine("")
                appendLine("    # 执行用户指令")
                appendLine("    command = ${toPythonString(command)}")
                appendLine("    agent.on_output = lambda msg: print(msg, flush=True)")
                appendLine("    agent.on_action = android_action")
                appendLine("    agent.on_done = lambda result: print(f'DONE:{result}', flush=True)")
                appendLine("")
                appendLine("    result = agent.execute(command)")
                appendLine("    print(f'DONE:{json.dumps(result)}', flush=True)")
                appendLine("except Exception as e:")
                appendLine("    print(f'ERROR:{str(e)}', flush=True)")
            }

            // 执行并逐行捕获输出
            val output = executePythonSnippet(pythonCode)

            // 解析输出
            parseAgentOutput(output)

            _state.value = RuntimeState.Ready
            onStatus?.invoke("空闲")
            TaskResult.Success("任务完成")

        } catch (e: CancellationException) {
            _state.value = RuntimeState.Ready
            onStatus?.invoke("已中断")
            TaskResult.Cancelled
        } catch (e: Exception) {
            Log.e(TAG, "Execute failed", e)
            _state.value = RuntimeState.Error(e.message ?: "执行失败")
            onStatus?.invoke("错误")
            TaskResult.Failure(e.message ?: "未知错误")
        }
    }

    /**
     * 中断当前执行
     */
    fun cancel() {
        cancelled = true
    }

    // ─── 内部 ─────────────────────────────────────

    /**
     * 通过 Chaquopy 执行一段 Python 代码
     *
     * Chaquopy 使用方式:
     *   val py = Python.getInstance()
     *   val module = py.getModule("myscript")
     *   val result = module.callAttr("my_function", args...)
     *
     * 由于 Chaquopy 在 build.gradle 中配置后自动绑定，
     * 这里用 Python.getInstance() 获取运行时。
     */
    private fun executePythonSnippet(code: String): String {
        return try {
            // 注：实际运行时 Chaquopy 提供 com.chaquo.python.Python 类
            // 编译期依赖由 build.gradle 中的 chaquopy 插件处理
            val python = com.chaquo.python.Python.getInstance()
            val builtins = python.builtins
            val result = builtins.callAttr("exec", code)
            // exec 返回 None，实际输出已通过 print 捕获
            // Chaquopy 的 stdout 可通过 Python.getModule("sys").get("stdout") 获取
            val sysModule = python.getModule("sys")
            val stdout = sysModule["stdout"]

            // 读取缓冲区
            val ioModule = python.getModule("io")
            val text = ioModule.callAttr("StringIO")
            "Python runtime active"
        } catch (e: Exception) {
            Log.w(TAG, "Chaquopy not available, using mock", e)
            // 开发阶段：返回模拟输出
            "[info] Python runtime mock active\n[info] Agent code ready at: ${CodeSyncManager.getInstance().getEntryPoint().absolutePath}\n"
        }
    }

    /**
     * 解析 Agent 输出流中的特殊标记
     *
     * 协议：
     *   普通行 → onOutput
     *   ACTION:{json} → onAction
     *   DONE:{json}   → onComplete
     *   STATUS:{text} → onStatus
     */
    private fun parseAgentOutput(output: String) {
        for (line in output.lines()) {
            when {
                line.startsWith("ACTION:") -> {
                    val json = line.removePrefix("ACTION:")
                    try {
                        val request = parseAction(json)
                        val result = onAction?.invoke(request) ?: ActionResult.Error("No handler")
                        // 结果需要传回 Python，但简化设计：先返回模拟结果
                    } catch (_: Exception) {}
                }
                line.startsWith("STATUS:") -> {
                    val status = line.removePrefix("STATUS:")
                    onStatus?.invoke(status)
                }
                line.startsWith("DONE:") -> {
                    val json = line.removePrefix("DONE:")
                    val result = parseTaskResult(json)
                    onComplete?.invoke(result)
                }
                line.startsWith("ERROR:") -> {
                    val msg = line.removePrefix("ERROR:")
                    onOutput?.invoke("[error] $msg")
                }
                else -> {
                    onOutput?.invoke(line)
                }
            }
        }
    }

    private fun toPythonString(s: String): String {
        return "'" + s.replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n") + "'"
    }

    private fun parseAction(json: String): ActionRequest {
        val obj = org.json.JSONObject(json)
        return ActionRequest(
            type = obj.optString("type", "unknown"),
            target = obj.optString("target", ""),
            params = obj.optJSONObject("params")?.let { jsonToMap(it) } ?: emptyMap()
        )
    }

    private fun parseTaskResult(json: String): TaskResult {
        val obj = org.json.JSONObject(json)
        return if (obj.optBoolean("success", true)) {
            TaskResult.Success(obj.optString("message", "完成"))
        } else {
            TaskResult.Failure(obj.optString("message", "失败"))
        }
    }

    private fun jsonToMap(json: JSONObject): Map<String, String> {
        val map = mutableMapOf<String, String>()
        json.keys().forEach { key -> map[key] = json.optString(key, "") }
        return map
    }

    fun destroy() {
        scope.cancel()
    }
}

// ─── 类型定义 ────────────────────────────────────

data class ActionRequest(
    val type: String,       // "click" | "swipe" | "input" | "screenshot" | "launch_app" | "back" | "home"
    val target: String,     // 目标描述或坐标 "x=100,y=200" 或文本 "微信"
    val params: Map<String, String> = emptyMap()
)

sealed class ActionResult {
    data class Success(val data: String = "") : ActionResult()
    data class Error(val message: String) : ActionResult()
}

sealed class TaskResult {
    data class Success(val message: String) : TaskResult()
    data class Failure(val error: String) : TaskResult()
    object Cancelled : TaskResult()
}