package com.pocketagent.app.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.widget.NestedScrollView

/**
 * 展开的悬浮窗视图 - 半透明终端风格
 *
 * 布局:
 * ┌──────────────────────────────┐
 * │ ■ Pocket Agent    [－] [×]  │  ← 标题栏 (可拖拽)
 * ├──────────────────────────────┤
 * │ $ 正在分析指令...           │
 * │   → 步骤 1: 打开微信        │  ← 滚动终端 (实时流式)
 * │   → 步骤 2: 搜索联系人      │
 * │   → 步骤 3: 发送消息        │
 * │   ...                       │
 * └──────────────────────────────┘
 *
 * 包含:
 * - 顶部标题栏 (可拖拽移动窗口)
 * - 中间滚动的终端输出
 * - 底部状态栏
 * - 自定义滚动条 (扁平化)
 */
class ExpandedOverlayView(
    private val context: Context,
    private val onMinimize: () -> Unit,
    private val screenWidth: Int,
    private val screenHeight: Int
) : LinearLayout(context) {

    val headerView: View
    private val terminalText: TextView
    private val statusBar: TextView
    private val scrollView: NestedScrollView

    private val terminalBuffer = StringBuilder()

    init {
        orientation = VERTICAL

        // 整体背景
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(12).toFloat()
            setColor(Color.parseColor("#E61A1A2E"))
            setStroke(dp(0.5f).toInt(), Color.parseColor("#44FFFFFF"))
        }

        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        // ═══ 标题栏 ═══
        headerView = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(8), dp(8))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadii = floatArrayOf(
                    dp(12).toFloat(), dp(12).toFloat(), dp(12).toFloat(), dp(12).toFloat(), // top
                    0f, 0f, 0f, 0f                      // bottom
                )
                setColor(Color.parseColor("#331A1A2E"))
            }

            // 窗口标题
            val titleText = TextView(context).apply {
                text = "Pocket Agent"
                setTextColor(Color.parseColor("#CCFFFFFF"))
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }
            addView(titleText)

            // 折叠按钮
            val minimizeBtn = TextView(context).apply {
                text = "－"
                setTextColor(Color.parseColor("#AAFFFFFF"))
                textSize = 18f
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(2), dp(4), dp(2))
                setOnClickListener { onMinimize() }
            }
            addView(minimizeBtn)

            // 关闭按钮
            val closeBtn = TextView(context).apply {
                text = "×"
                setTextColor(Color.parseColor("#80FFFFFF"))
                textSize = 16f
                gravity = Gravity.CENTER
                setPadding(dp(6), dp(2), dp(8), dp(2))
                setOnClickListener { onMinimize() }
            }
            addView(closeBtn)
        }
        addView(headerView)

        // 分割线
        addView(View(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(0.5f)
            )
            setBackgroundColor(Color.parseColor("#22FFFFFF"))
        })

        // ═══ 终端内容 (可滚动) ═══
        scrollView = NestedScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            isFillViewport = true
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackgroundColor(Color.parseColor("#0D000000"))
        }

        terminalText = TextView(context).apply {
            setTextColor(Color.parseColor("#CCE0E0E0"))
            textSize = 11f
            typeface = android.graphics.Typeface.MONOSPACE
            setLineSpacing(2f, 1f)
            text = "$ Pocket Agent 就绪\n"
        }
        scrollView.addView(terminalText)
        addView(scrollView)

        // 分割线
        addView(View(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(0.5f)
            )
            setBackgroundColor(Color.parseColor("#22FFFFFF"))
        })

        // ═══ 底部状态栏 ═══
        statusBar = TextView(context).apply {
            text = "空闲"
            setTextColor(Color.parseColor("#4CAF50"))
            textSize = 10f
            setPadding(dp(12), dp(6), dp(12), dp(6))
            setBackgroundColor(Color.parseColor("#11000000"))
        }
        addView(statusBar)

        elevation = dp(16).toFloat()
    }

    // ─── 公开方法 ─────────────────────────────────

    fun appendStream(text: String) {
        post {
            // 只追加新增部分
            if (text.length > terminalBuffer.length) {
                val newText = text.substring(terminalBuffer.length)
                terminalBuffer.append(newText)

                val styled = formatTerminalText(terminalBuffer.toString())
                terminalText.text = styled

                // 自动滚动到底部
                scrollView.post {
                    scrollView.fullScroll(View.FOCUS_DOWN)
                }
            }
        }
    }

    fun updateStatus(status: String) {
        post {
            val color = when {
                status.contains("错误") || status.contains("失败") -> Color.parseColor("#F44336")
                status.contains("完成") -> Color.parseColor("#4CAF50")
                status.contains("执行") || status.contains("运行") -> Color.parseColor("#2196F3")
                status.contains("思考") -> Color.parseColor("#FFC107")
                else -> Color.parseColor("#4CAF50")
            }
            statusBar.text = status
            statusBar.setTextColor(color)
        }
    }

    /**
     * 终端文本格式化 - 给不同行加颜色
     */
    private fun formatTerminalText(text: String): SpannableStringBuilder {
        val result = SpannableStringBuilder()
        val lines = text.split("\n")

        for (line in lines) {
            val start = result.length

            when {
                line.startsWith("[step") -> {
                    result.append("  → $line\n")
                    result.setSpan(
                        ForegroundColorSpan(Color.parseColor("#81C784")),
                        start, result.length,
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                line.startsWith("[task]") -> {
                    result.append("  $line\n")
                    result.setSpan(
                        ForegroundColorSpan(Color.parseColor("#64B5F6")),
                        start, result.length,
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                line.startsWith("[error]") || line.startsWith("❌") -> {
                    result.append("  $line\n")
                    result.setSpan(
                        ForegroundColorSpan(Color.parseColor("#EF5350")),
                        start, result.length,
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                line.startsWith("[done]") -> {
                    result.append("  $line\n")
                    result.setSpan(
                        ForegroundColorSpan(Color.parseColor("#66BB6A")),
                        start, result.length,
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                line.startsWith("[info]") -> {
                    result.append("  $line\n")
                    result.setSpan(
                        ForegroundColorSpan(Color.parseColor("#90A4AE")),
                        start, result.length,
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                line.startsWith("$") -> {
                    result.append("$line\n")
                    result.setSpan(
                        ForegroundColorSpan(Color.parseColor("#00E5FF")),
                        start, result.length,
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                else -> {
                    result.append("  $line\n")
                }
            }
        }

        return result
    }

    private fun dp(value: Float): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }

    private fun dp(value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }
}