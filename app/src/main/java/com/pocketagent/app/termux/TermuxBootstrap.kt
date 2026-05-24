package com.pocketagent.app.termux

import android.util.Log

/**
 * Termux 环境引导 — 检查 Termux 是否已安装并可用
 */
object TermuxBootstrap {
    private const val TAG = "TermuxBootstrap"

    val termuxRoot: String
        get() = "/data/data/com.termux/files"
    val termuxUsr: String
        get() = "$termuxRoot/usr"

    val isReady: Boolean
        get() {
            val bash = java.io.File("$termuxUsr/bin/bash")
            val ready = bash.exists()
            Log.d(TAG, "Termux ready: $ready")
            return ready
        }
}