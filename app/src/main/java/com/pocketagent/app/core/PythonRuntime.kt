package com.pocketagent.app.core

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Python 运行时管理 — 预留扩展接口
 *
 * 当前版本使用 update/PythonRuntime.kt 作为主运行时实现。
 * 本对象保留作为未来 Chaquopy 集成的入口点。
 */
object PythonRuntime {
    private const val TAG = "PythonRuntime"

    private var runtimeDir: File? = null
    private var initialized = false

    fun bootstrap(context: Context) {
        runtimeDir = File(context.filesDir, CodeSyncManager.RUNTIME_DIR)
        initialized = true
        Log.i(TAG, "Python runtime bootstrapped (stub). Runtime dir: ${runtimeDir?.absolutePath}")
    }

    fun loadAgent(): Any? {
        Log.w(TAG, "loadAgent: stub, Chaquopy not integrated")
        return null
    }

    fun runAgent(userInput: String, actionHandler: ActionHandler): String {
        Log.d(TAG, "runAgent (stub): $userInput")
        return "Python runtime not available (Chaquopy not integrated)"
    }

    class ActionHandler {
        fun click(x: Int, y: Int): String {
            Log.d(TAG, "click($x, $y)")
            return """{"status":"ok"}"""
        }

        fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, duration: Int): String {
            Log.d(TAG, "swipe($x1,$y1 → $x2,$y2, ${duration}ms)")
            return """{"status":"ok"}"""
        }

        fun input(text: String): String {
            Log.d(TAG, "input: $text")
            return """{"status":"ok"}"""
        }

        fun screenshot(): String {
            Log.d(TAG, "screenshot requested")
            return """{"status":"ok","data":""}"""
        }

        fun openApp(packageName: String): String {
            Log.d(TAG, "openApp: $packageName")
            return """{"status":"ok"}"""
        }

        fun back(): String {
            Log.d(TAG, "back pressed")
            return """{"status":"ok"}"""
        }

        fun home(): String {
            Log.d(TAG, "home pressed")
            return """{"status":"ok"}"""
        }

        fun streamOutput(text: String) {
            StreamBridge.out(text)
        }
    }
}