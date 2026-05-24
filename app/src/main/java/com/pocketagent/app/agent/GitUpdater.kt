package com.pocketagent.app.agent

import android.content.Context
import android.util.Log
import com.pocketagent.app.update.CodeSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Git 更新器 — 从主仓库 Release 下载最新 Agent 代码
 *
 * 流程：
 *   1. 检查本地缓存是否过期
 *   2. 从 GitHub Release 下载 agent_core.zip
 *   3. 解压到 runtime 目录
 *   4. 更新本地版本标记
 */
class GitUpdater(private val context: Context) {
    private val TAG = "GitUpdater"

    enum class UpdateStatus {
        UPDATED,        // 成功更新到新版本
        SEED_FALLBACK,  // 使用内置种子代码
        FAILED          // 更新失败
    }

    data class UpdateResult(
        val status: UpdateStatus,
        val version: String? = null,
        val message: String = ""
    )

    /**
     * 执行更新
     */
    suspend fun run(): UpdateResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "GitUpdater.run()")
        // 当前版本使用内置种子代码，不进行网络更新
        val syncManager = CodeSyncManager.init(context, "")
        val seedResult = syncManager.ensureSeedCode()
        return@withContext if (seedResult) {
            UpdateResult(
                status = UpdateStatus.SEED_FALLBACK,
                version = "seed",
                message = "使用内置 Agent 代码"
            )
        } else {
            UpdateResult(
                status = UpdateStatus.FAILED,
                message = "内置种子代码加载失败"
            )
        }
    }

    companion object {
        // 主仓库 Release URL
        const val RELEASE_BASE_URL = "https://github.com/Dreamt0511/Pocket-Agent/releases/latest/download"
        const val AGENT_CORE_ZIP = "agent_core.zip"
    }
}