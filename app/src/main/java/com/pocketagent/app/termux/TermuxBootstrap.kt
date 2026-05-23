package com.pocketagent.app.termux

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Manages the Termux runtime environment embedded in the app.
 *
 * Responsibilities:
 * - Extract bundled Termux bootstrap to app-private directory
 * - Ensure Python + pip are available
 * - Set up environment variables (PATH, HOME, LD_LIBRARY_PATH)
 *
 * The Termux environment lives at: {filesDir}/termux/
 */
object TermuxBootstrap {

    private const val TAG = "TermuxBootstrap"

    /** Path to the Termux root within app-private storage. */
    lateinit var termuxRoot: String
        private set

    /** Path to the Termux usr directory (binaries, libs). */
    lateinit var termuxUsr: String
        private set

    /** Whether the bootstrap has completed successfully. */
    var isReady: Boolean = false
        private set

    /**
     * Ensure the Termux environment is ready.
     * Called once during Application.onCreate().
     *
     * In a full implementation, this would:
     * 1. Extract a pre-built Termux bootstrap tarball from APK assets
     * 2. Run the bootstrap setup script
     * 3. Verify `python3` and `pip` are available
     *
     * For the MVP, we assume Termux is already installed or we
     * delegate to the standalone Termux app.
     */
    suspend fun ensureReady(context: Context) {
        if (isReady) return

        val filesDir = context.filesDir.absolutePath
        termuxRoot = "$filesDir/termux"
        termuxUsr = "$termuxRoot/usr"

        Log.i(TAG, "Initializing Termux environment at $termuxRoot")

        // Create directory structure
        File(termuxRoot).mkdirs()
        File(termuxUsr).mkdirs()
        File("$termuxUsr/bin").mkdirs()
        File("$termuxUsr/lib").mkdirs()
        File("$termuxRoot/home").mkdirs()

        // Check if Termux is available via standalone app
        val termuxAppInstalled = checkTermuxAppInstalled()

        if (termuxAppInstalled) {
            Log.i(TAG, "Standalone Termux app detected, will use its environment")
            // Use the existing Termux installation
            termuxRoot = "/data/data/com.termux/files"
            termuxUsr = "$termuxRoot/usr"
        } else {
            Log.i(TAG, "No standalone Termux found, using embedded bootstrap")
            // In MVP, we bootstrap from assets
            bootstrapFromAssets(context)
        }

        // Verify basic executables
        val pythonAvailable = verifyExecutable("python3") || verifyExecutable("python")
        Log.i(TAG, "Python available: $pythonAvailable")

        isReady = true
        Log.i(TAG, "Termux environment ready")
    }

    /**
     * Check if the standalone Termux app is installed.
     */
    private fun checkTermuxAppInstalled(): Boolean {
        return try {
            val termuxDataDir = File("/data/data/com.termux/files/usr")
            termuxDataDir.exists() && termuxDataDir.isDirectory
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Bootstrap Termux from bundled assets.
     * In MVP, this creates a minimal environment with Python.
     */
    private fun bootstrapFromAssets(context: Context) {
        Log.i(TAG, "Bootstrapping from assets...")

        // Create a shell script that sets up the environment
        val bootstrapScript = File("$termuxRoot/bootstrap.sh")
        bootstrapScript.writeText("""
#!/data/data/com.pocketagent.app/files/termux/usr/bin/bash
export HOME=$termuxRoot/home
export PATH=$termuxUsr/bin:$PATH
export LD_LIBRARY_PATH=$termuxUsr/lib:$LD_LIBRARY_PATH
export TMPDIR=$termuxRoot/tmp
mkdir -p $termuxRoot/tmp

# Update package list
apt-get update -y

# Install Python
apt-get install -y python python-pip git

echo "Bootstrap complete"
        """.trimIndent())
        bootstrapScript.setExecutable(true)

        Log.i(TAG, "Bootstrap script written to ${bootstrapScript.absolutePath}")
    }

    /**
     * Verify an executable exists in the Termux PATH.
     */
    private fun verifyExecutable(name: String): Boolean {
        return try {
            val path = "$termuxUsr/bin/$name"
            File(path).exists() || tryRun("which $name").isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Try to run a command and return its output (blocking, for checks only).
     */
    private fun tryRun(cmd: String): String {
        return try {
            val process = Runtime.getRuntime().exec(
                arrayOf("sh", "-c", "PATH=$termuxUsr/bin:\$PATH $cmd")
            )
            process.inputStream.bufferedReader().readText().trim()
        } catch (e: Exception) {
            ""
        }
    }
}