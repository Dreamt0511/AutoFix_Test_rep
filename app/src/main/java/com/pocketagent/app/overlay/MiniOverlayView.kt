package com.pocketagent.app.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

/**
 * 迷你悬浮窗视图 (40dp 圆形)
 *
 * 布局:
 * ┌────────────────┐
 * │  [icon] 状态点  │  ← 整个圆形可点击展开
 * └────────────────┘
 *
 * 特性:
 * - 半透明深色背景 + 圆角
 * - 中心图标 (app icon)
 * - 右下角状态指示灯 (绿=运行, 黄=思考, 红=错误)
 * - 长按拖拽移动
 * - 点击展开
 */
class MiniOverlayView(
    private val context: Context,
    private val onToggleExpand: () -> Unit,
    private val onMoveToCorner: () -> Unit
) : FrameLayout(context) {

    private val iconView: ImageView
    private val statusDot: View
    private val statusText: TextView
    private var pulseAnimator: ValueAnimator? = null

    init {
        val iconSize = dp(36)
        val containerSize = dp(44)

        layoutParams = LayoutParams(containerSize, containerSize)

        // 背景
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#CC1A1A2E"))
            setStroke(dp(1.5f), Color.parseColor("#33FFFFFF"))
        }

        // 图标
        iconView = ImageView(context).apply {
            layoutParams = LayoutParams(iconSize, iconSize).apply {
                gravity = Gravity.CENTER
            }
            setImageResource(android.R.drawable.ic_menu_manage) // 替换为真实 logo
            setColorFilter(Color.WHITE)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            alpha = 0.9f
        }
        addView(iconView)

        // 状态指示灯
        statusDot = View(context).apply {
            val dotSize = dp(8)
            layoutParams = LayoutParams(dotSize, dotSize).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                setMargins(0, 0, dp(6), dp(6))
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#4CAF50"))
            }
        }
        addView(statusDot)

        // 状态文本 (显示在旁边)
        statusText = TextView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.START
                setMargins(0, 0, 0, dp(2))
            }
            setTextColor(Color.WHITE)
            textSize = 9f
            maxLines = 1
            alpha = 0f // 默认隐藏
            text = ""
        }
        addView(statusText)

        setOnClickListener {
            onToggleExpand()
        }

        elevation = dp(8).toFloat()
    }

    fun updateStatus(status: String, isOperating: Boolean) {
        post {
            val color = when {
                isOperating -> Color.parseColor("#FFC107") // 操作中 - 黄色
                status.contains("错误") || status.contains("失败") -> Color.parseColor("#F44336") // 错误 - 红色
                status.contains("完成") -> Color.parseColor("#4CAF50") // 完成 - 绿色
                status.contains("执行") || status.contains("运行") -> Color.parseColor("#2196F3") // 运行 - 蓝色
                else -> Color.parseColor("#4CAF50") // 空闲 - 绿色
            }

            (statusDot.background as? GradientDrawable)?.setColor(color)

            // 运行时脉动动画
            if (isOperating || status.contains("运行")) {
                startPulse(color)
            } else {
                stopPulse()
            }

            statusText.text = status.take(6)
        }
    }

    private fun startPulse(color: Int) {
        if (pulseAnimator?.isRunning == true) return

        pulseAnimator = ValueAnimator.ofFloat(1f, 1.3f).apply {
            duration = 600
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animator ->
                val scale = animator.animatedValue as Float
                statusDot.scaleX = scale
                statusDot.scaleY = scale
                (statusDot.background as GradientDrawable).alpha = (255 * (1.5f - scale)).toInt()
            }
            start()
        }
    }

    private fun stopPulse() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        statusDot.scaleX = 1f
        statusDot.scaleY = 1f
        (statusDot.background as GradientDrawable).alpha = 255
    }

    private fun dp(value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }

    private fun dp(value: Float): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }
}