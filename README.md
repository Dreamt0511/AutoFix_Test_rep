# Pocket-Agent Android App

> 你的手机 AI 操作员。用自然语言让 AI 自主操控手机。

## 这是什么？

Pocket-Agent 是一个 Android 应用，让你用自然语言告诉 AI 你想做什么，AI 会自动操控你的手机完成任务。

**例子：**
- "去抖音给周杰伦主页的前 3 个视频点赞"
- "去咸鱼搜索索尼 a6000，找 1700 左右的，问卖家是否顺丰包邮"
- "打开微信给张三发'明天见'"

## 快速开始

### 1. 下载安装

从 [GitHub Releases](https://github.com/Dreamt0511/Pocket-Agent/releases) 下载最新 APK 并安装。

### 2. 配置

打开 App → 进入设置 → 填写你的 OpenAI 兼容 API key。

支持任何 OpenAI 兼容接口：OpenAI、Claude、DeepSeek、国内大模型 API 等。

### 3. 准备工作

- 安装 Pocket-Agent MCP 服务 App（提供无障碍操控能力）
- 在系统设置中为 MCP 服务开启无障碍权限

### 4. 开始使用

在首页输入你想让 AI 完成的任务，点击发送即可。

## 从源码构建

```bash
# 克隆仓库
git clone https://github.com/Dreamt0511/Pocket-Agent.git
cd Pocket-Agent

# 构建 APK
./gradlew assembleDebug

# APK 在 app/build/outputs/apk/debug/
```

## 项目结构

```
Pocket-Agent-Android/
├── app/                          # Android 应用壳
│   └── src/main/java/.../
│       ├── agent/                # Agent 桥接层
│       │   ├── AgentBridge.kt    # 与 Python agent 通信
│       │   └── GitUpdater.kt     # Git 自动更新
│       ├── termux/               # Termux 集成层
│       │   ├── TermuxBootstrap.kt # 环境初始化
│       │   ├── TermuxBridge.kt   # Shell 执行桥
│       │   └── TermuxService.kt  # 后台保活
│       ├── ui/                   # Compose UI
│       │   ├── screens/          # 页面 (Home/Setup/Task/Terminal)
│       │   ├── navigation/       # 导航
│       │   └── theme/            # 主题
│       └── data/                 # 数据存储
├── agent-seed/                   # Python agent 种子代码
│   ├── stable_entry.py           # 不变入口（壳调用此文件）
│   ├── agent/                    # Agent 核心模块
│   │   ├── core.py               # 主循环
│   │   ├── planner.py            # LLM 任务规划
│   │   ├── executor.py           # 操作执行
│   │   └── mcp_client.py         # MCP 通信客户端
│   ├── tasks/                    # 任务定义
│   ├── config.json               # 配置文件
│   └── requirements.txt          # Python 依赖
└── scripts/                      # Shell 脚本
    ├── bootstrap.sh              # 环境初始化
    └── update.sh                 # 手动更新
```

## 更新机制

应用采用「壳 + Git 运行时」架构：

1. **APK = 不变壳**：UI、Termux 环境、Git 客户端
2. **Agent 代码 = 从 GitHub 动态拉取**：每次启动自动 `git pull`
3. **种子代码 = 离线兜底**：首次安装或无网时使用内置种子

更新 Agent 只需 push 到仓库 main 分支，用户下次打开 App 自动生效。

## 许可

MIT License