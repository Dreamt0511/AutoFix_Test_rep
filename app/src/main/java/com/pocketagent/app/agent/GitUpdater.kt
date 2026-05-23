package com.pocketagent.app.agent

import android.content.Context
import android.util.Log
import com.pocketagent.app.termux.TermuxBridge
import com.pocketagent.app.termux.TermuxBootstrap
import java.io.File

/**
 * Handles updating the Python agent code from the GitHub repository.
 *
 * Strategy (Hybrid A+B):
 * - APK ships with a "seed" snapshot of the agent code in assets
 * - On first launch, seed code is extracted to app-private directory
 * - On every subsequent launch, `git pull` in background to get latest
 * - If git pull fails (offline, no git), seed code serves as fallback
 * - `stable_entry.py` is the immutable interface between APK and agent
 *
 * The agent code lives at: {filesDir}/agent-seed/
 * Git remote: https://github.com/Dreamt0511/Pocket-Agent
 */
class GitUpdater(private val context: Context) {

    companion object {
        private const val TAG = "GitUpdater"
        private const val REPO_URL = "https://github.com/Dreamt0511/Pocket-Agent.git"
        private const val AGENT_DIR = "agent-seed"
    }

    private val termuxBridge = TermuxBridge()
    private val agentPath: String
        get() = "${context.filesDir}/$AGENT_DIR"

    /**
     * Run the update flow:
     * 1. If agent directory doesn't exist, extract seed from assets
     * 2. Run git pull to get latest
     * 3. Install/update Python dependencies
     * 4. Verify stable_entry.py exists
     */
    suspend fun run(): UpdateResult {
        Log.i(TAG, "Starting update flow...")

        // Step 1: Ensure agent directory exists
        val agentDir = File(agentPath)
        if (!agentDir.exists() || !File("$agentPath/stable_entry.py").exists()) {
            Log.i(TAG, "Agent directory missing, extracting seed...")
            extractSeed()
        }

        // Step 2: Try git pull
        val hasGit = checkGitAvailable()
        if (hasGit) {
            val pullResult = gitPull()
            if (pullResult.success) {
                Log.i(TAG, "Git pull successful")

                // Step 3: Update Python dependencies
                updateDependencies()

                return UpdateResult(
                    status = UpdateStatus.UPDATED,
                    message = "Agent updated to latest version"
                )
            } else {
                Log.w(TAG, "Git pull failed: ${pullResult.output}")
            }
        } else {
            Log.w(TAG, "Git not available, using seed code")
        }

        // Fallback: verify seed code is functional
        if (File("$agentPath/stable_entry.py").exists()) {
            return UpdateResult(
                status = UpdateStatus.SEED_FALLBACK,
                message = "Using bundled agent code (offline or git unavailable)"
            )
        }

        return UpdateResult(
            status = UpdateStatus.FAILED,
            message = "No agent code available"
        )
    }

    /**
     * Extract the seed agent code from APK assets to the app-private directory.
     */
    private fun extractSeed() {
        val agentDir = File(agentPath)
        agentDir.mkdirs()

        // In MVP, the seed code is pre-placed in the assets directory.
        // The actual extraction copies agent-seed/ from assets to filesDir.
        try {
            val assetsSeedPath = "agent-seed"
            copyAssets(context, assetsSeedPath, agentPath)
            Log.i(TAG, "Seed code extracted to $agentPath")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract seed: ${e.message}")
            // Create minimal stable_entry.py as emergency fallback
            createEmergencyEntryPoint()
        }
    }

    /**
     * Recursively copy assets directory to target path.
     */
    private fun copyAssets(context: Context, assetPath: String, targetPath: String) {
        val assetManager = context.assets
        val files = assetManager.list(assetPath) ?: return

        for (file in files) {
            val assetFile = if (assetPath.isEmpty()) file else "$assetPath/$file"
            val targetFile = File("$targetPath/$file")

            // Check if it's a directory (has sub-files)
            val subFiles = assetManager.list(assetFile)
            if (subFiles != null && subFiles.isNotEmpty()) {
                targetFile.mkdirs()
                copyAssets(context, assetFile, targetFile.absolutePath)
            } else {
                // It's a file, copy it
                try {
                    assetManager.open(assetFile).use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to copy asset $assetFile: ${e.message}")
                }
            }
        }
    }

    /**
     * Create a minimal stable_entry.py as an emergency fallback
     * when even the seed extraction fails.
     */
    private fun createEmergencyEntryPoint() {
        val entryFile = File("$agentPath/stable_entry.py")
        entryFile.parentFile?.mkdirs()
        entryFile.writeText("""
#!/usr/bin/env python3
\"\"\"
Pocket-Agent Stable Entry Point (Emergency Fallback)
This file is auto-generated and provides minimal functionality.
\"\"\"
import sys
import json

def main():
    mode = "--mode=task" in sys.argv
    if mode:
        print(json.dumps({
            "type": "error",
            "message": "Agent code not yet downloaded. Please check your network connection and restart the app."
        }))
    else:
        print(json.dumps({"type": "ready", "message": "Emergency fallback active"}))

if __name__ == "__main__":
    main()
        """.trimIndent())
        Log.w(TAG, "Emergency entry point created")
    }

    /**
     * Check if git is available in the Termux environment.
     */
    private suspend fun checkGitAvailable(): Boolean {
        return try {
            val result = termuxBridge.execute("which git")
            result.success && result.output.isNotBlank()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Run git pull in the agent directory.
     * If the directory is not a git repo yet, clone it first.
     */
    private suspend fun gitPull(): com.pocketagent.app.termux.CommandResult {
        val agentDir = File(agentPath)

        // Check if it's already a git repo
        val isGitRepo = File("$agentPath/.git").exists()

        return if (isGitRepo) {
            Log.d(TAG, "Running git pull...")
            termuxBridge.execute("cd $agentPath && git pull origin main")
        } else {
            // First time: we have seed code extracted, but no git repo.
            // Initialize git and set remote (keep our seed files).
            Log.d(TAG, "Initializing git in seed directory...")
            termuxBridge.execute(
                "cd $agentPath && " +
                "git init && " +
                "git remote add origin $REPO_URL && " +
                "git fetch origin main && " +
                "git reset --hard origin/main"
            )
        }
    }

    /**
     * Install/update Python dependencies from requirements.txt.
     */
    private suspend fun updateDependencies() {
        val reqFile = "$agentPath/requirements.txt"
        if (File(reqFile).exists()) {
            Log.d(TAG, "Updating Python dependencies...")
            termuxBridge.execute(
                "cd $agentPath && pip install -r requirements.txt --quiet"
            )
        }
    }
}

enum class UpdateStatus {
    UPDATED,        // Successfully pulled latest from GitHub
    SEED_FALLBACK,  // Using bundled seed code (offline or no git)
    FAILED          // No agent code available at all
}

data class UpdateResult(
    val status: UpdateStatus,
    val message: String
)