package com.pocketagent.app.update

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/**
 * 代码同步引擎 — 从主仓库动态拉取最新 Agent Python 代码
 *
 * 保证 App 不写死：主仓库代码更新后 App 自动同步，无需重新编译 APK。
 *
 * 工作流程：
 *  1. 启动时检查本地版本 vs 远程版本 (version.json)
 *  2. 版本不一致 → 下载 agent_latest.zip
 *  3. 解压到 App 私有目录 (app_python_runtime/)
 *  4. 加载到 PythonRuntime 执行
 *
 * 使用方式：
 *   CodeSyncManager.init(context, "https://github.com/xxx/pocket-agent/releases")
 *   CodeSyncManager.syncIfNeeded()
 */
class CodeSyncManager(private val context: Context) {

    companion object {
        private const val TAG = "CodeSyncManager"
        private const val RUNTIME_DIR = "app_python_runtime"
        private const val VERSION_FILE = "version.json"
        private const val AGENT_ZIP = "agent_latest.zip"

        @Volatile
        private var instance: CodeSyncManager? = null

        fun init(context: Context, repoUrl: String): CodeSyncManager {
            return instance ?: synchronized(this) {
                instance ?: CodeSyncManager(context.applicationContext).also {
                    it.repoBaseUrl = repoUrl
                    instance = it
                }
            }
        }

        fun getInstance(): CodeSyncManager =
            instance ?: throw IllegalStateException("CodeSyncManager not initialized. Call init() first.")
    }

    var repoBaseUrl: String = ""
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    sealed class SyncState {
        object Idle : SyncState()
        object Checking : SyncState()
        object Downloading : SyncState()
        object Extracting : SyncState()
        data class Ready(val version: String) : SyncState()
        data class Error(val message: String) : SyncState()
    }

    /** 运行时目录: /data/data/<package>/files/app_python_runtime/ */
    fun getRuntimeDir(): File {
        return File(context.filesDir, RUNTIME_DIR).also { it.mkdirs() }
    }

    /** 版本文件本地路径 */
    private fun getLocalVersionFile(): File = File(getRuntimeDir(), VERSION_FILE)

    /** 获取本地版本号 */
    fun getLocalVersion(): String {
        val file = getLocalVersionFile()
        if (!file.exists()) return "0.0.0"
        return try {
            JSONObject(file.readText()).optString("version", "0.0.0")
        } catch (_: Exception) {
            "0.0.0"
        }
    }

    // ─── 同步流程 ─────────────────────────────────

    /**
     * 主入口：检查并同步最新代码
     *
     * @param force 是否强制同步（忽略版本比较）
     */
    suspend fun syncIfNeeded(force: Boolean = false): SyncResult = withContext(Dispatchers.IO) {
        try {
            _syncState.value = SyncState.Checking

            // 1. 获取远程版本信息
            val remoteVersion = fetchRemoteVersion()
            val localVersion = if (force) "0.0.0" else getLocalVersion()

            Log.d(TAG, "Remote: $remoteVersion, Local: $localVersion")

            if (!force && remoteVersion == localVersion) {
                _syncState.value = SyncState.Ready(localVersion)
                return@withContext SyncResult.UpToDate(localVersion)
            }

            // 2. 下载最新代码包
            _syncState.value = SyncState.Downloading
            val zipUrl = "$repoBaseUrl/download/latest/$AGENT_ZIP"
            val zipFile = File(context.cacheDir, AGENT_ZIP)
            downloadFile(zipUrl, zipFile)

            // 3. 解压到运行时目录
            _syncState.value = SyncState.Extracting
            extractZip(zipFile, getRuntimeDir())

            // 4. 保存新版本号
            getLocalVersionFile().writeText("""{"version":"$remoteVersion"}""")

            // 5. 清理缓存
            zipFile.delete()

            _syncState.value = SyncState.Ready(remoteVersion)
            Log.i(TAG, "Synced to version $remoteVersion")
            SyncResult.Synced(remoteVersion)

        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            _syncState.value = SyncState.Error(e.message ?: "Unknown")
            SyncResult.Failed(e.message ?: "同步失败")
        }
    }

    // ─── 远程版本查询 ─────────────────────────────

    private suspend fun fetchRemoteVersion(): String {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$repoBaseUrl/download/latest/$VERSION_FILE"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: throw Exception("Empty response")
                    JSONObject(body).optString("version", "0.0.0")
                } else {
                    // 回退：尝试 GitHub API
                    fetchGitHubLatestVersion()
                }
            } catch (e: Exception) {
                fetchGitHubLatestVersion()
            }
        }
    }

    private fun fetchGitHubLatestVersion(): String {
        // 从 GitHub API 获取 latest release tag
        val apiUrl = repoBaseUrl
            .replace("https://github.com/", "https://api.github.com/repos/")
            .replace("/releases", "") + "/releases/latest"

        return try {
            val request = Request.Builder()
                .url(apiUrl)
                .addHeader("Accept", "application/vnd.github.v3+json")
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "")
                json.optString("tag_name", "0.0.0").trimStart('v')
            } else {
                "0.0.0"
            }
        } catch (_: Exception) {
            "0.0.0"
        }
    }

    // ─── 文件下载 ─────────────────────────────────

    private fun downloadFile(url: String, destFile: File) {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("下载失败: HTTP ${response.code}")
        }

        response.body?.byteStream()?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw Exception("下载失败: 空响应体")
    }

    // ─── Zip 解压 ────────────────────────────────

    private fun extractZip(zipFile: File, destDir: File) {
        // 清空旧文件（保留 version.json）
        destDir.listFiles()?.forEach { file ->
            if (file.name != VERSION_FILE) {
                file.deleteRecursively()
            }
        }

        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val entryFile = File(destDir, entry.name)

                if (entry.isDirectory) {
                    entryFile.mkdirs()
                } else {
                    entryFile.parentFile?.mkdirs()
                    FileOutputStream(entryFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }

                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    /** 获取入口脚本路径 */
    fun getEntryPoint(): File {
        return File(getRuntimeDir(), "main.py")
    }

    /** 检查入口脚本是否存在 */
    fun isCodeReady(): Boolean {
        return getEntryPoint().exists()
    }
}

// ─── 同步结果 ────────────────────────────────────

sealed class SyncResult {
    data class UpToDate(val version: String) : SyncResult()
    data class Synced(val version: String) : SyncResult()
    data class Failed(val error: String) : SyncResult()
}