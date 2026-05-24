# 主仓库 Python 代码模板 — agent_core/main.py

# 此文件会被打包为 agent_latest.zip 发布在 GitHub Releases
# App 启动时通过 CodeSyncManager 自动下载并动态加载

import json
import sys
from typing import Callable, Optional


class Agent:
    """
    主仓库 Agent 核心 — 在 Android APK 内通过 Chaquopy 动态执行

    不依赖任何 Android 特定库，通过回调接口与 Kotlin 层通信：
    - on_output(msg)   → 日志/流式输出 → 悬浮窗 + 终端
    - on_action(json)  → Android 能力请求 → AccessibilityService
    - on_done(json)    → 任务完成通知

    主仓库更新流程：
    1. 开发者修改此文件及 tools/ 下的工具模块
    2. 打 tag 并 release: agent_latest.zip
    3. App 下次启动时 CodeSyncManager 自动拉取
    4. APK 无需重新编译
    """

    def __init__(self):
        self.on_output: Optional[Callable[[str], None]] = None
        self.on_action: Optional[Callable[[str], str]] = None
        self.on_done: Optional[Callable[[str], None]] = None

        # 加载工具模块
        self.tools = {}
        self._load_tools()

    def _load_tools(self):
        """动态加载 tools/ 下的工具模块"""
        import os
        import importlib

        tools_dir = os.path.join(os.path.dirname(__file__), "tools")
        if os.path.isdir(tools_dir):
            for filename in os.listdir(tools_dir):
                if filename.endswith(".py") and not filename.startswith("_"):
                    module_name = filename[:-3]
                    try:
                        mod = importlib.import_module(f"tools.{module_name}")
                        if hasattr(mod, "register"):
                            mod.register(self)
                        self.tools[module_name] = mod
                        self._log(f"[info] 工具加载: {module_name}")
                    except Exception as e:
                        self._log(f"[warn] 工具 {module_name} 加载失败: {e}")

    def _log(self, msg: str):
        """输出到日志流"""
        if self.on_output:
            self.on_output(msg + "\n")

    def _act(self, action_type: str, target: str, **params) -> str:
        """请求 Android 能力"""
        if self.on_action:
            payload = json.dumps({
                "type": action_type,
                "target": target,
                "params": params
            })
            return self.on_action(payload)
        return "{}"

    # ─── 核心执行引擎 ──────────────────────────

    def execute(self, command: str) -> dict:
        """
        执行用户的自然语言指令

        子类可重写此方法接入自己的 LLM 计划器。
        这里提供一个占位实现，实际使用时替换为你的 Agent 逻辑。
        """
        self._log(f"[step] 正在分析: {command}")

        # 模拟计划步骤
        steps = self._plan(command)

        for i, step in enumerate(steps, 1):
            self._log(f"[step] 步骤 {i}/{len(steps)}: {step['description']}")

            if step.get("action"):
                result = self._act(
                    step["action"],
                    step.get("target", ""),
                    **(step.get("params", {}))
                )
                self._log(f"  → 结果: {result}")

        return {"success": True, "message": "任务完成"}

    def _plan(self, command: str) -> list:
        """
        计划器 — 将自然语言指令分解为可执行步骤

        实际项目中这里接入 LLM (通过 HTTP API)，返回步骤列表。
        """
        # 占位实现：简单关键词匹配
        if "微信" in command and "发" in command:
            return [
                {"description": "打开微信", "action": "launch_app", "target": "com.tencent.mm"},
                {"description": "搜索联系人", "action": "click", "target": "搜索"},
                {"description": "输入消息并发送", "action": "input", "target": command},
            ]
        elif "设置" in command:
            return [
                {"description": "打开系统设置", "action": "launch_app", "target": "com.android.settings"},
            ]
        else:
            return [
                {"description": f"执行: {command}", "action": None},
            ]


# ─── 入口 ─────────────────────────────────────

if __name__ == "__main__":
    agent = Agent()
    agent.on_output = lambda msg: print(msg, end="", flush=True)

    # 从命令行参数获取指令
    cmd = " ".join(sys.argv[1:]) if len(sys.argv) > 1 else "查看手机状态"
    result = agent.execute(cmd)
    print(f"DONE:{json.dumps(result)}")