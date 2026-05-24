# Pocket Agent Android App — 编译清单

> 版本：v1.0 | 更新日期：2026-05-24 | 目标仓库：[GitHub](https://github.com/Dreamt0511/Pocket-Agent-Android)

---

## 一、环境准备检查

| 检查项 | 要求 | 检查命令 | 状态 |
|--------|------|----------|------|
| Java JDK | 17 | `java -version` | ☐ |
| JAVA_HOME 已设置 | 指向 JDK 17 | `echo $env:JAVA_HOME` | ☐ |
| Android SDK | API 33 (Android 13) | `%ANDROID_HOME%\platforms\android-33` | ☐ |
| Build Tools | 32.0.0 | `%ANDROID_HOME%\build-tools\32.0.0` | ☐ |
| ANDROID_HOME 已设置 | SDK 根路径 | `echo $env:ANDROID_HOME` | ☐ |
| `local.properties` 存在 | `sdk.dir` 正确 | 项目根目录 | ☐ |
| 磁盘空间 | ≥ 5 GB | 用户目录 ~/.gradle | ☐ |
| 网络连接 | 可访问 Google Maven | 推荐开启代理/VPN | ☐ |

---

## 二、项目文件完整性检查

| 文件 / 目录 | 作用 | 状态 |
|-------------|------|------|
| `settings.gradle.kts` | 项目模块声明 | ☐ |
| `build.gradle.kts` | 根构建配置 | ☐ |
| `gradle.properties` | Gradle 属性 | ☐ |
| `local.properties` | SDK 路径 | ☐ |
| `gradlew.bat` | Windows Wrapper 脚本 | ☐ |
| `gradle/wrapper/gradle-wrapper.properties` | Wrapper 版本配置 | ☐ |
| `gradle/wrapper/gradle-wrapper.jar` | Wrapper JAR | ☐ |
| `.gitignore` | Git 忽略规则 | ☐ |
| `app/build.gradle.kts` | 应用模块构建 | ☐ |
| `app/proguard-rules.pro` | 混淆规则 | ☐ |
| `app/src/main/AndroidManifest.xml` | 清单文件 | ☐ |

---

## 三、应用清单关键配置检查

| 检查项 | 位置 | 说明 |
|--------|------|------|
| `package` | `com.pocketagent.app` | 应用包名 |
| `minSdk` | 26 | 最低 API 26 (Android 8.0) |
| `targetSdk` | 33 | 目标 API 33 (Android 13) |
| `compileSdk` | 33 | 编译 SDK 33 |
| `SYSTEM_ALERT_WINDOW` 权限 | AndroidManifest.xml | 悬浮窗必需 |
| `FOREGROUND_SERVICE` 权限 | AndroidManifest.xml | 后台服务必需 |

> **架构说明**：Pocket Agent 不内置无障碍服务。手机操控（触控/无障碍）由独立的 [NeuralBridge MCP](https://github.com/dondetir/NeuralBridge_mcp) 服务完成，本项目通过 MCP 协议与之通信。
| `RECORD_AUDIO` 权限 | AndroidManifest.xml | 语音输入 |
| `INTERNET` 权限 | AndroidManifest.xml | 网络通信 |
| `OverlayService` 注册 | AndroidManifest.xml | 悬浮窗服务 |
| `AgentService` 注册 | AndroidManifest.xml | Agent 后台服务 |

---

## 四、编译步骤

### 步骤 1：配置本地 SDK 路径

编辑项目根目录 `local.properties`：
```properties
sdk.dir=C\:\\Program Files (x86)\\Android\\android-sdk
```

### 步骤 2：设置环境变量（PowerShell）

```powershell
$env:JAVA_HOME = "D:\JDK17"
$env:ANDROID_HOME = "C:\Program Files (x86)\Android\android-sdk"
```

### 步骤 3：编译 Debug APK

```bash
cd PocketAgent
.\gradlew.bat assembleDebug
```

### 步骤 4：验证产物

编译成功后产物位于：
```
app\build\outputs\apk\debug\app-debug.apk
```

---

## 五、常见问题排查

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| `Could not determine Java version` | JDK 版本不匹配 | 确保 `$env:JAVA_HOME` 指向 JDK 17 |
| `SDK location not found` | SDK 路径未配置 | 检查 `local.properties` 中 `sdk.dir` |
| `Failed to install platform 33` | 缺少 API 33 | SDK Manager 安装 `platforms;android-33` |
| `Failed to find Build Tools 32.0.0` | 缺少构建工具 | SDK Manager 安装 `build-tools;32.0.0` |
| `Could not resolve all files` | 网络不通 | 开启代理/VPN，重试 |
| `foregroundServiceType "specialUse"` 报错 | API 33 不支持 | 已在清单中适配，如仍报错可删除该属性 |
| `gradlew.bat 无法运行` | Wrapper 不完整 | 重新运行 `gradle wrapper` |
| `Permission denied` | gradlew.bat 无执行权限 | `Unblock-File gradlew.bat` |
| `OutOfMemoryError` | Gradle 内存不足 | 修改 `gradle.properties` 增大 `-Xmx4096m` |

---

## 六、安装与运行

```bash
# 安装到已连接的 Android 设备
adb install app\build\outputs\apk\debug\app-debug.apk

# 或复制 APK 到手机直接安装
```

### 安装后首次启动

1. 授权「显示在其他应用上层」权限
2. 授权「录音」权限（语音输入）
3. 进入「配置」页面，填写 MCP 服务地址（NeuralBridge）及 API Key
4. 主页测试 Agent 对话

---

## 七、核心模块说明

| 模块 | 路径 | 职责 |
|------|------|------|
| 应用入口 | `MainActivity.kt` | Compose 导航容器 |
| 全局悬浮窗 | `overlay/` | 6 个文件，MINI/EXPANDED 双模式 |
| Agent 服务 | `service/AgentService.kt` | 前台服务 + 任务调度 |
| Python 运行时 | `core/PythonRuntime.kt` | Chaquopy 集成 |
| 代码同步 | `core/CodeSyncManager.kt` | GitHub Releases 动态加载 |
| 种子代码 | `agent-seed/` | Python Agent 核心 + 配置 |
| UI 层 | `ui/screens/` | 7 个屏幕（Home/Chat/Config/Terminal/Overlay/History/Onboarding） |

---

## 八、开发分支建议

| 分支 | 用途 |
|------|------|
| `master` | 稳定发布版本 |
| `dev` | 日常开发集成 |
| `feature/*` | 功能开发分支 |
| `hotfix/*` | 紧急修复分支 |

---

*如有编译问题，请提交 Issue 到 [GitHub 仓库](https://github.com/Dreamt0511/Pocket-Agent-Android/issues)。*