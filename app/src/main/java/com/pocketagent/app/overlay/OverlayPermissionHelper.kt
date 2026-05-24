package com.pocketagent.app.overlay

import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * 悬浮窗权限检查与引导 (可嵌入 SettingsScreen 或首次引导)
 */
object OverlayPermissionHelper {

    /**
     * 检查是否已授予 SYSTEM_ALERT_WINDOW
     */
    fun isGranted(): Boolean {
        // 需要通过 Context，这里仅提供判断逻辑，调用方传入 context
        return false // 占位 — 调用方需提供 context 版本
    }

    /**
     * 检查并返回是否需要申请
     */
    fun needsRequest(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }

    /**
     * 生成权限设置页的 Intent Uri
     */
    fun getPermissionSettingsUri(packageName: String): Uri {
        return Uri.parse("package:$packageName")
    }
}