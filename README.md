# Pocket Agent Android App

> 将 Pocket-Agent Python AI Agent 能力搬到 Android 手机上的原生客户端。

## 项目结构

```
PocketAgent/
├── app/                          # Android 主模块
│   ├── build.gradle.kts         # 模块构建配置
│   ├── proguard-rules.pro       # 混淆规则
│   └── src/main/
│       ├── AndroidManifest.xml   # 清单文件
│       ├── java/com/pocketagent/app/
│       │   ├── MainActivity.kt
│       │   ├── PocketAgentApplication.kt
│       │   ├── accessibility/
│       │   │   └── AgentAccessibilityService.kt
│       │   ├── bridge/
│       │   │   └── PythonBridge.kt
│       │   ├── config/
│       │   │   └── ConfigManager.kt
│       │   ├── daemon/
│       │   │   └── AgentDaemon.kt
│       │   ├── network/
│       │   │   └── NetworkMonitor.kt
│       │   ├── overlay/
│       │   │   ├── OverlayManager.kt
│       │   │   ├── OverlayService.kt
│       │   │   ├── MiniOverlayView.kt
│       │   │   └── ExpandedOverlayView.kt
│       │   ├── service/
│       │   │   └── AgentService.kt
│       │   ├── stream/
│       │   │   └── StreamBridge.kt
│       │   ├── sync/
│       │   │   ├── CodeSyncManager.kt
│       │   │   └── UpdateChecker.kt
│       │   ├── task/
│       │   │   └── TaskQueueManager.kt
│       │   └── ui/
│       │       ├── theme/
│       │       ├── navigation/
│       │       └── screens/
│       │           ├── HomeScreen.kt
│       │           ├── ChatScreen.kt
│       │           ├── ConfigScreen.kt
│       │           ├── TerminalScreen.kt
│       │           ├── OverlayScreen.kt
│       │           ├── HistoryScreen.kt
│       │           └── onboarding/
│       └── res/
│           ├── values/
│           │   ├── strings.xml
│           │   ├── colors.xml
│           │   └── themes.xml
│           ├── xml/
│           │   ├── backup_rules.xml
│           │   ├── data_extraction_rules.xml
│           │   └── accessibility_service_config.xml
│           └── mipmap-*/
├── agent-seed/                   # Python Agent 种子代码（运行时动态加载）
│   ├── stable_entry.py          # 多模式启动入口
│   ├── config.json              # Agent 配置
│   ├── requirements.txt         # Python 依赖
│   └── agent/
│       ├── config.py
│       ├── core.py
│       ├── prompts/
│       └── tools/
├── build.gradle.kts             # 根构建文件
├── settings.gradle.kts          # 项目设置
├── gradle.properties            # Gradle 配置
├── local.properties             # 本地 SDK 路径
├── gradlew.bat                  # Gradle Wrapper (Windows)
└── gradle/wrapper/
    ├── gradle-wrapper.jar
    └── gradle-wrapper.properties
```

## 编译环境要求

| 组件 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 17 | OpenJDK 或 Oracle JDK |
| Android SDK | API 33 (Android 13) | 最低 API 33 |
| Build Tools | 32.0.0 | |
| Gradle | 8.5 | 项目自带 wrapper，无需手动安装 |
| Kotlin | 1.9.22 | AGP 自动管理 |
| AGP | 8.2.2 | Android Gradle Plugin |

## 快速编译（命令行）

### 1. 设置环境变量

```bash
# Windows PowerShell
$env:JAVA_HOME = "你的 JDK 17 路径"
$env:ANDROID_HOME = "你的 Android SDK 路径"
$env:PATH = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:PATH"
```

### 2. 配置 SDK 路径

编辑 `local.properties`，填入你的 SDK 路径：
```
sdk.dir=C\:\\YourAndroidSdkPath
```

### 3. 编译

```bash
# Debug APK
gradlew.bat assembleDebug

# 或者（如果 gradlew 不可用，下载 Gradle 后）
gradle assembleDebug
```

**如果没有 gradlew.jar**，从 [Gradle Releases](https://services.gradle.org/distributions/gradle-8.5-bin.zip) 下载 Gradle 8.5，解压后把 `bin/` 目录加入 PATH，然后运行 `gradle assembleDebug`。

### 4. 产物位置

```
app/build/outputs/apk/debug/app-debug.apk
```

## 使用 Android Studio 编译（推荐）

1. 打开 Android Studio → File → Open → 选择项目根目录
2. 等待 Gradle 同步完成（首次需下载依赖，约 5-10 分钟）
3. Build → Build Bundle(s) / APK(s) → Build APK(s)
4. APK 生成在 `app/build/outputs/apk/debug/`

## 注意事项

1. **首次编译**：会从 Google Maven 和 Gradle Plugin Portal 下载大量依赖，需要稳定的网络连接
2. **如果有 VPN**：建议开启，Google 服务需要科学上网
3. **AndroidManifest 中的 foregroundServiceType="specialUse"** 需要 API 34+，如果编译报错，可改为 API 33 兼容的类型或直接移除该属性
4. **Chaquopy Python 插件**：当前版本未包含。如需嵌入 Python 运行时（让 App 自带 Python 环境执行 Agent 代码），需在 `app/build.gradle.kts` 中添加：
   ```kotlin
   plugins {
       id("com.chaquopy") version "15.0.1"
   }
   ```
   并在 defaultConfig 中添加 python 配置块。

## 核心功能

- **AI 对话**：连接主仓库 Pocket-Agent 的 Python 后端，实现手机端 AI Agent
- **全局悬浮窗**：系统级悬浮球 + 终端卡片，退出 App 后仍可监控 Agent 执行
- **守护进程**：后台服务保活，任务队列调度
- **语音输入**：内置语音识别
- **无障碍服务**：支持操控其他 App
- **动态代码加载**：主仓库更新后 App 自动拉取最新 Python Agent 代码
- **配置管理**：支持主/子 Agent 模型配置，云端 API + 本地 llama-server