package com.pocketagent.app.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * 动态代码同步管理器 — GitHub Releases 动态加载
 *
 * 流程:
 *   1. 检查本地版本 (sync_config.json)
 *   2. 查询 GitHub Releases 最新版本
 *   3. 若有更新 → 下载 agent_latest.zip
 *   4. 解压到 app_python_runtime/
 *   5. 写入版本文件
 *
 * 主仓库: https://github.com/Dreamt0511/Pocket-Agent
 */
object CodeSyncManager {
    private const val TAG = "CodeSyncManager"

    private const val GITHUB_REPO = "Dreamt0511/Pocket-Agent"
    private const val RELEASES_API = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
    private const val SYNC_CONFIG = "sync_config.json"
    private const val RUNTIME_DIR = "app_python_runtime"

    private var context: Context? = null

    suspend fun sync(ctx: Context) = withContext(Dispatchers.IO) {
        context = ctx.applicationContext
        val runtimeDir = File(ctx.filesDir, RUNTIME_DIR)
        if (!runtimeDir.exists()) {
            // 首次安装 — 必须下载
            Log.i(TAG, "First install detected, downloading agent code...")
            downloadAndExtract(runtimeDir)
        } else {
            // 检查更新
            val currentVersion = readLocalVersion(ctx)
            val latestVersion = fetchLatestVersion()
            if (latestVersion != null && latestVersion != currentVersion) {
                Log.i(TAG, "Update available: $currentVersion → $latestVersion")
                downloadAndExtract(runtimeDir)
                writeLocalVersion(ctx, latestVersion)
            } else {
                Log.i(TAG, "Agent code is up to date (v$currentVersion)")
            }
        }
    }

    fun forceSync(ctx: Context) {
        // 同步调用版本（用于 UI 层面的强制更新）
        Thread {
            try {
                val runtimeDir = File(ctx.filesDir, RUNTIME_DIR)
                runtimeDir.deleteRecursively()
                downloadAndExtract(runtimeDir)
            } catch (e: Exception) {
                Log.e(TAG, "Force sync failed", e)
            }
        }.start()
    }

    // ─── 本地版本管理 ───────────────────────

    private fun readLocalVersion(ctx: Context): String {
        return try {
            val configFile = File(ctx.filesDir, SYNC_CONFIG)
            if (!configFile.exists()) return "0.0.0"
            val json = JSONObject(configFile.readText())
            json.optString("version", "0.0.0")
        } catch (e: Exception) {
            "0.0.0"
        }
    }

    private fun writeLocalVersion(ctx: Context, version: String) {
        val configFile = File(ctx.filesDir, SYNC_CONFIG)
        val json = JSONObject().apply { put("version", version) }
        configFile.writeText(json.toString(2))
    }

    // ─── 远程版本查询 ───────────────────────

    private fun fetchLatestVersion(): String? {
        return try {
            val conn = URL(RELEASES_API).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000

            if (conn.responseCode != 200) return null

            val body = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(body)
            json.optString("tag_name", null)?.removePrefix("v")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch latest version", e)
            null
        }
    }

    // ─── 下载与解压 ────────────────────────

    private fun downloadAndExtract(runtimeDir: File) {
        runtimeDir.mkdirs()

        // 查找最新 Release 中 agent_latest.zip 的下载 URL
        val downloadUrl = fetchAssetDownloadUrl() ?: throw IOException("Failed to get download URL")

        val zipFile = File(runtimeDir, "agent_latest.zip")

        // 下载
        Log.i(TAG, "Downloading agent code from $downloadUrl")
        val conn = URL(downloadUrl).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 30_000
        conn.readTimeout = 120_000

        conn.inputStream.use { input ->
            FileOutputStream(zipFile).use { output ->
                input.copyTo(output)
            }
        }

        // 解压
        Log.i(TAG, "Extracting agent code...")
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val targetFile = File(runtimeDir, entry.name)
                if (entry.isDirectory) {
                    targetFile.mkdirs()
                } else {
                    targetFile.parentFile?.mkdirs()
                    FileOutputStream(targetFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        // 清理 zip
        zipFile.delete()

        // 标记版本
        val version = fetchLatestVersion() ?: "latest"
        writeLocalVersion(context!!, version)

        Log.i(TAG, "Agent code synced: v$version, files in ${runtimeDir.absolutePath}")
    }

    private fun fetchAssetDownloadUrl(): String? {
        return try {
            val conn = URL(RELEASES_API).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            conn.connectTimeout = 10_000

            val body = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(body)
            val assets = json.optJSONArray("assets") ?: return null

            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.getString("name") == "agent_latest.zip") {
                    return asset.getString("browser_download_url")
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch asset URL", e)
            null
        }
    }
}