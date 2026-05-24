package com.pocketagent.app.core

import android.content.Context
import android.util.Log
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import java.io.File

/**
 * Chaquopy Python 运行时 — 动态加载主仓库 Agent 代码
 *
 * 架构:
 *   Android 能力层 (无障碍/悬浮窗/UI)
 *     ↕ ActionRequest / ActionResult 协议
 *   Chaquopy Python 解释器
 *     ↕ 执行 agent_core_main.py
 *   动态加载的 Python Agent 代码 (从主仓库 Release 下载)
 *
 * 协议:
 *   Python → Android: 通过 Python 对象上的 @JvmStatic 方法回调
 *   Android → Python: 通过 PyObject.callAttr() 调用 Python 函数
 */
object PythonRuntime {
    private const val TAG = "PythonRuntime"

    private var python: Python? = null
    private var agentModule: com.chaquo.python.PyObject? = null
    private var runtimeDir: File? = null

    fun bootstrap(context: Context) {
        runtimeDir = File(context.filesDir, CodeSyncManager.RUNTIME_DIR)

        // 初始化 Chaquopy
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
        python = Python.getInstance()

        // 将 runtime 目录加入 sys.path
        val sys = python!!.getModule("sys")
        val path = sys["path"].asList()
        if (!path.contains(runtimeDir!!.absolutePath)) {
            sys["path"].asList().callAttr("insert", 0, runtimeDir!!.absolutePath)
        }

        Log.i(TAG, "Python runtime bootstrapped. Runtime dir: ${runtimeDir!!.absolutePath}")
    }

    /**
     * 加载主 Agent 模块
     */
    fun loadAgent(): com.chaquo.python.PyObject? {
        if (python == null || runtimeDir == null) return null
        return try {
            val module = python!!.getModule("agent_core_main")
            agentModule = module
            Log.i(TAG, "Agent module loaded: agent_core_main")
            module
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load agent module", e)
            null
        }
    }

    /**
     * 执行 Agent — 传入用户指令，返回结果
     */
    fun runAgent(userInput: String, actionHandler: ActionHandler): String {
        if (agentModule == null) loadAgent()
        if (agentModule == null) return "Agent 模块加载失败"

        return try {
            // 注入 Android 能力层回调
            agentModule!!["android_handler"] = actionHandler

            // 调用 agent.run(userInput)
            val result = agentModule!!.callAttr("run", userInput)
            result.toString()
        } catch (e: Exception) {
            "Python 执行错误: ${e.message}"
        }
    }

    /**
     * Android 能力层 — 暴露给 Python Agent 调用的接口
     *
     * Python 端通过 `android_handler.perform_action(action_json)` 调用
     * 返回 JSON: {"status": "ok"/"error", "data": ...}
     */
    class ActionHandler {
        // 无障碍点击
        fun click(x: Int, y: Int): String {
            // TODO: 接入 AccessibilityService
            Log.d(TAG, "click($x, $y)")
            return """{"status":"ok"}"""
        }

        // 无障碍滑动
        fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, duration: Int): String {
            Log.d(TAG, "swipe($x1,$y1 → $x2,$y2, ${duration}ms)")
            return """{"status":"ok"}"""
        }

        // 无障碍输入
        fun input(text: String): String {
            Log.d(TAG, "input: $text")
            return """{"status":"ok"}"""
        }

        // 截图
        fun screenshot(): String {
            // TODO: MediaProjection 截图 → Base64
            Log.d(TAG, "screenshot requested")
            return """{"status":"ok","data":""}"""
        }

        // 打开 App
        fun openApp(packageName: String): String {
            Log.d(TAG, "openApp: $packageName")
            return """{"status":"ok"}"""
        }

        // 按返回键
        fun back(): String {
            Log.d(TAG, "back pressed")
            return """{"status":"ok"}"""
        }

        // 按 Home 键
        fun home(): String {
            Log.d(TAG, "home pressed")
            return """{"status":"ok"}"""
        }

        // 流式输出
        fun streamOutput(text: String) {
            StreamBridge.out(text)
        }
    }
}

// ─── Python 端模板 (agent_core_main.py) ────────
// 此模板随 APK 内置，作为兜底；
// 正常运行时由 CodeSyncManager 从主仓库 Release 下载覆盖。
//
// ```python
// # agent_core_main.py — Pocket Agent Python 核心
// from agent.agent_langchain import LangChainPocketAgent
// from agent.config import MAX_ITERATIONS, PROJECT_ROOT
// ...
// def run(user_input: str) -> str:
//     ...
// ```