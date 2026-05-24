package com.pocketagent.app.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.FrameLayout
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow

/**
 * 全局悬浮窗服务 - 退到后台后仍悬浮在所有应用上层
 *
 * 双模式:
 * - MINI:   40dp 圆形图标，可拖拽，显示执行状态
 * - EXPANDED: 半透明终端卡片，实时流式输出，占屏幕 1/3
 *
 * 无障碍协作: 检测到 Agent 正在操控手机时自动切到 MINI 并移到右上角
 */
class OverlayService : Service() {

    companion object {
        const val ACTION_SHOW = "com.pocketagent.app.overlay.SHOW"
        const val ACTION_HIDE = "com.pocketagent.app.overlay.HIDE"
        const val ACTION_EXPAND = "com.pocketagent.app.overlay.EXPAND"
        const val ACTION_MINIMIZE = "com.pocketagent.app.overlay.MINIMIZE"

        // 全局状态流 - 供外部写入
        val overlayState = kotlinx.coroutines.flow.MutableStateFlow(OverlayMode.HIDDEN)
        val streamText = kotlinx.coroutines.flow.MutableStateFlow("")
        val taskStatus = kotlinx.coroutines.flow.MutableStateFlow("空闲")

        // 无障碍操作信号
        val isAgentOperating = kotlinx.coroutines.flow.MutableStateFlow(false)
    }

    enum class OverlayMode { HIDDEN, MINI, EXPANDED }

    private lateinit var windowManager: WindowManager
    private lateinit var miniView: MiniOverlayView
    private lateinit var expandedView: ExpandedOverlayView

    private var miniParams: WindowManager.LayoutParams? = null
    private var expandedParams: WindowManager.LayoutParams? = null
    private var currentMode = OverlayMode.MINI

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // 屏幕尺寸
    private var screenWidth = 0
    private var screenHeight = 0

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // 获取屏幕尺寸
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            screenWidth = metrics.bounds.width()
            screenHeight = metrics.bounds.height()
        } else {
            val display = windowManager.defaultDisplay
            val metrics = android.util.DisplayMetrics()
            @Suppress("DEPRECATION")
            display.getMetrics(metrics)
            screenWidth = metrics.widthPixels
            screenHeight = metrics.heightPixels
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> showMini()
            ACTION_HIDE -> hideAll()
            ACTION_EXPAND -> showExpanded()
            ACTION_MINIMIZE -> showMini()
            else -> showMini()
        }

        // 监听无障碍操作信号
        observeAgentOperation()

        overlayState.value = currentMode
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ─── 迷你悬浮窗 ─────────────────────────────────

    private fun showMini() {
        // 先移除展开视图
        expandedParams?.let { try { windowManager.removeView(expandedView) } catch (_: Exception) {} }
        expandedParams = null

        if (miniParams == null) {
            miniView = MiniOverlayView(
                context = this,
                onToggleExpand = { showExpanded() },
                onMoveToCorner = { moveMiniToCorner() }
            )

            miniParams = WindowManager.LayoutParams().apply {
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }

                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

                format = PixelFormat.TRANSLUCENT
                width = WindowManager.LayoutParams.WRAP_CONTENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                gravity = Gravity.TOP or Gravity.START
                x = screenWidth - dpToPx(60)
                y = screenHeight / 3
            }

            setupMiniDrag()
            windowManager.addView(miniView, miniParams)
        }

        // 更新迷你视图的状态文本
        serviceScope.launch {
            taskStatus.collect { status ->
                miniView.updateStatus(
                    status = status,
                    isOperating = isAgentOperating.value
                )
            }
        }

        currentMode = OverlayMode.MINI
        overlayState.value = OverlayMode.MINI
    }

    private fun setupMiniDrag() {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false
        val dragThreshold = 10f

        miniView.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = miniParams!!.x
                    initialY = miniParams!!.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY

                    if (kotlin.math.abs(deltaX) > dragThreshold ||
                        kotlin.math.abs(deltaY) > dragThreshold) {
                        isDragging = true
                    }

                    if (isDragging) {
                        miniParams!!.x = initialX + deltaX.toInt()
                        miniParams!!.y = initialY + deltaY.toInt()

                        // 边界限制
                        val viewW = view.width
                        val viewH = view.height
                        miniParams!!.x = miniParams!!.x.coerceIn(0, screenWidth - viewW)
                        miniParams!!.y = miniParams!!.y.coerceIn(0, screenHeight - viewH - dpToPx(80))

                        windowManager.updateViewLayout(miniView, miniParams)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // 如果只是点击（没有拖拽），则展开
                    if (!isDragging) {
                        miniView.performClick()
                    }
                    true
                }
                else -> false
            }
        }
    }

    fun moveMiniToCorner() {
        miniParams?.let { params ->
            params.x = screenWidth - dpToPx(60)
            params.y = dpToPx(20)
            try { windowManager.updateViewLayout(miniView, params) } catch (_: Exception) {}
        }
    }

    // ─── 展开悬浮窗 ─────────────────────────────────

    private fun showExpanded() {
        // 先移除迷你视图
        miniParams?.let { try { windowManager.removeView(miniView) } catch (_: Exception) {} }
        miniParams = null

        if (expandedParams == null) {
            expandedView = ExpandedOverlayView(
                context = this,
                onMinimize = { showMini() },
                screenWidth = screenWidth,
                screenHeight = screenHeight
            )

            expandedParams = WindowManager.LayoutParams().apply {
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }

                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

                format = PixelFormat.TRANSLUCENT
                width = (screenWidth * 0.9).toInt()
                height = (screenHeight * 0.4).toInt()
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = dpToPx(60)
            }

            setupExpandedDrag()
            windowManager.addView(expandedView, expandedParams)
        }

        // 监听流式文本
        serviceScope.launch {
            streamText.collect { text ->
                expandedView.appendStream(text)
            }
        }

        // 监听状态
        serviceScope.launch {
            taskStatus.collect { status ->
                expandedView.updateStatus(status)
            }
        }

        currentMode = OverlayMode.EXPANDED
        overlayState.value = OverlayMode.EXPANDED
    }

    private fun setupExpandedDrag() {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        expandedView.headerView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = expandedParams!!.x
                    initialY = expandedParams!!.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    expandedParams!!.x = initialX + (event.rawX - initialTouchX).toInt()
                    expandedParams!!.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(expandedView, expandedParams)
                    true
                }
                else -> false
            }
        }
    }

    // ─── 无障碍协作 ─────────────────────────────────

    private fun observeAgentOperation() {
        serviceScope.launch {
            isAgentOperating.collect { operating ->
                if (operating && currentMode == OverlayMode.EXPANDED) {
                    // Agent 正在操作 → 缩到迷你模式并移到角落
                    showMini()
                    moveMiniToCorner()
                }
            }
        }
    }

    // ─── 公开 API ────────────────────────────────────

    fun hideAll() {
        miniParams?.let { try { windowManager.removeView(miniView) } catch (_: Exception) {} }
        expandedParams?.let { try { windowManager.removeView(expandedView) } catch (_: Exception) {} }
        miniParams = null
        expandedParams = null
        currentMode = OverlayMode.HIDDEN
        overlayState.value = OverlayMode.HIDDEN
        stopSelf()
    }

    /**
     * 追加流式输出文本（供 AgentDaemon 等外部调用）
     */
    fun appendStreamText(text: String) {
        streamText.value += text
    }

    fun updateTaskStatus(status: String) {
        taskStatus.value = status
    }

    // ─── 工具方法 ────────────────────────────────────

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        hideAll()
        super.onDestroy()
    }
}