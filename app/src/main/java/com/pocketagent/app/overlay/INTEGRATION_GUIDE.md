# 悬浮窗集成指南

## 目录结构

```
app/src/main/java/com/pocketagent/app/overlay/
├── OverlayService.kt       # 悬浮窗后台服务 (双模式: MINI / EXPANDED)
├── MiniOverlayView.kt      # 迷你浮动球 (40dp圆形，可拖拽，状态指示灯)
├── ExpandedOverlayView.kt  # 展开终端视图 (半透明，实时流式输出，语法着色)
├── OverlayManager.kt       # 全局管理器 (单例，对外统一接口)
└── StreamBridge.kt         # 流式输出桥接器 (Agent输出 → 悬浮窗 + 终端 + 历史)
```

---

## 1. AndroidManifest.xml — 所需修改

在 `<manifest>` 节点下添加悬浮窗权限:

```xml
<!-- 悬浮窗权限 (必须) -->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />

<!-- 前台服务权限 (Android 9+) -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
```

在 `<application>` 节点下注册 Service:

```xml
<!-- 悬浮窗服务 -->
<service
    android:name=".overlay.OverlayService"
    android:exported="false"
    android:foregroundServiceType="specialUse">
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="overlay_for_agent_status_display" />
</service>
```

---

## 2. PocketAgentApp.kt — Application 初始化

在 `onCreate()` 中添加:

```kotlin
import com.pocketagent.app.overlay.OverlayManager

class PocketAgentApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // ⭐ 初始化悬浮窗管理器
        OverlayManager.init(this)
        // ... 其他初始化
    }
}
```

---

## 3. MainActivity.kt — 权限申请与生命周期

### 3.1 首次启动时检查并请求悬浮窗权限

```kotlin
import android.os.Build
import android.provider.Settings
import com.pocketagent.app.overlay.OverlayManager

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 检查悬浮窗权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(this)) {
            // 跳转到系统权限设置页
            OverlayManager.openOverlayPermissionSettings(this)
        }

        // ... 其他初始化
    }
}
```

### 3.2 退后台时启动悬浮窗

```kotlin
override fun onPause() {
    super.onPause()
    // App 进入后台 → 启动迷你悬浮窗
    if (!isFinishing) {
        OverlayManager.with(this).showMini()
    }
}

override fun onResume() {
    super.onResume()
    // App 回到前台 → 隐藏悬浮窗
    OverlayManager.with(this).hide()
}
```

---

## 4. AgentService.kt / AgentDaemon.kt — 集成流式输出

在 Agent 执行任务的关键节点调用 `StreamBridge`:

```kotlin
import com.pocketagent.app.overlay.StreamBridge

class AgentDaemon {

    suspend fun executeTask(userCommand: String): TaskResult {
        // 1. 标记开始
        StreamBridge.status("分析指令...")
        StreamBridge.out("$ ${userCommand}\n")

        // 2. 解析指令
        val plan = parseCommand(userCommand)
        StreamBridge.step(1, "解析完成: ${plan.totalSteps}个步骤")

        // 3. 逐步执行
        for ((index, step) in plan.steps.withIndex()) {
            StreamBridge.status("执行步骤 ${index + 1}/${plan.totalSteps}")

            // ⭐ 标记即将操控手机 → 悬浮窗自动最小化
            if (step.requiresUITouch) {
                StreamBridge.signalOperation(true)
            }

            try {
                val result = executeStep(step)
                StreamBridge.out("  → ${step.description}")
                StreamBridge.out("    结果: ${result.summary}\n")

                if (step.requiresUITouch) {
                    Thread.sleep(500) // 给无障碍操作留缓冲
                }
            } catch (e: Exception) {
                StreamBridge.error(e.message ?: "未知错误")
                StreamBridge.signalOperation(false)
                return TaskResult.Failure(e)
            } finally {
                StreamBridge.signalOperation(false)
            }
        }

        // 4. 完成
        StreamBridge.status("完成")
        StreamBridge.done("任务执行完毕")
        return TaskResult.Success()
    }
}
```

---

## 5. TerminalScreen.kt — 注册为流式目标（可选）

如果想让终端屏幕也实时显示相同输出:

```kotlin
import com.pocketagent.app.overlay.StreamTarget
import com.pocketagent.app.overlay.StreamBridge

class TerminalScreen {

    private val terminalTarget = object : StreamTarget {
        override fun onOutput(line: String) {
            appendToTerminal(line)
        }

        override fun onStatusChange(status: String) {
            updateTerminalStatus(status)
        }

        override fun onAgentOperation(active: Boolean) {
            if (active) {
                showOverlayWarning("Agent 正在操控手机...")
            } else {
                hideOverlayWarning()
            }
        }
    }

    fun onScreenEnter() {
        StreamBridge.register(terminalTarget)
    }

    fun onScreenLeave() {
        StreamBridge.unregister(terminalTarget)
    }

    private fun appendToTerminal(line: String) {
        // 追加到 Compose 终端 TextField 或 View
    }
}
```

---

## 6. 悬浮窗 API 速查

| 方法 | 说明 |
|------|------|
| `OverlayManager.with(ctx).showMini()` | 显示迷你浮动球 |
| `OverlayManager.with(ctx).showExpanded()` | 展开终端视图 |
| `OverlayManager.with(ctx).minimize()` | 折叠为迷你模式 |
| `OverlayManager.with(ctx).hide()` | 完全隐藏 |
| `StreamBridge.out(msg)` | 输出普通日志 |
| `StreamBridge.step(n, desc)` | 输出步骤 |
| `StreamBridge.task(desc)` | 输出任务描述 |
| `StreamBridge.info(msg)` | 输出信息 |
| `StreamBridge.error(msg)` | 输出错误 |
| `StreamBridge.done(msg)` | 输出完成 |
| `StreamBridge.status(s)` | 更新状态条 |
| `StreamBridge.signalOperation(bool)` | 通知 Agent 正在操控手机 |

---

## 7. 核心设计决策

### 7.1 悬浮窗与无障碍操作共存

当 `StreamBridge.signalOperation(true)` 被调用时:
- `OverlayService` 监听到 `isAgentOperating = true`
- 如果当前是 EXPANDED 模式 → 自动折叠为 MINI 并移动到右上角
- 如果当前是 MINI 模式 → 移到右上角避免遮挡
- 无障碍服务完成点击后，调用 `signalOperation(false)` 解除

### 7.2 线程安全

- `OverlayService.streamText` 使用 `MutableStateFlow`，天然线程安全
- `appendStream()` 只追加新增部分，避免重复渲染

### 7.3 性能

- MINI 模式仅 44dp 圆形视图，零开销
- EXPANDED 模式使用 `NestedScrollView` + `SpannableStringBuilder`，仅增量更新
- 颜色格式化按行处理，不阻塞主线程