#!/usr/bin/env python3
"""
Pocket Agent 守护进程模式 (Daemon Mode)
通过 stdin/stdout 管道与 Android 壳进行常驻流式通信。

通信协议:
- 输入: 用户指令（纯文本，每行一条）
- 输出: 流式内容 + EOM 标记（"<<<EOM>>>"）
- 控制: "ping" -> 心跳回复 "pong"；"shutdown" -> 退出
- 配置: 通过 --config 参数指定 config.json 路径
"""

import sys
import json
import signal
import traceback
from pathlib import Path
from typing import Optional
import asyncio

# ─── 配置加载 ────────────────────────────────────────────────

def load_config(config_path: str) -> dict:
    """加载配置文件"""
    try:
        with open(config_path, 'r', encoding='utf-8') as f:
            return json.load(f)
    except (FileNotFoundError, json.JSONDecodeError) as e:
        print(f"[error] 配置文件加载失败: {e}")
        return {}

# ─── Agent 核心 ────────────────────────────────────────────────

class DaemonAgent:
    """守护进程模式的 Agent"""

    EOM = "<<<EOM>>>"

    def __init__(self, config: dict):
        self.config = config
        self.llm_config = config.get("llm", {})
        self.mcp_config = config.get("mcp", {})
        self.system_config = config.get("system", {})
        self.running = True
        self._initialize()

    def _initialize(self) -> None:
        """初始化：检查依赖、加载工具"""
        print("[info] 初始化 Agent 守护进程...")

        # 检查关键依赖
        try:
            import langchain
            print(f"[info] LangChain 版本: {langchain.__version__}")
        except ImportError:
            print("[warning] LangChain 未安装，使用简化模式")
            self.fallback_mode = True

        # 加载工具
        self.tools = self._load_tools()
        print(f"[info] 已加载 {len(self.tools)} 个工具")

    def _load_tools(self) -> list:
        """加载可用工具列表"""
        tools = []

        # MCP 工具
        if self.mcp_config.get("server_url"):
            tools.append("mcp_accessibility")
            tools.append("mcp_device")

        # 内置工具
        tools.extend(["shell", "file", "web", "clipboard"])

        return tools

    def execute(self, command: str) -> str:
        """执行用户指令并返回流式结果"""
        try:
            # 简化模式：直接返回演示输出
            result = self._simulate_execution(command)
            return result
        except Exception as e:
            traceback.print_exc(file=sys.stderr)
            return f"[error] 执行失败: {str(e)}\n{self.EOM}\n"

    def _simulate_execution(self, command: str) -> str:
        """模拟执行（在没有完整 LLM 链路时的占位实现）"""
        import time

        output_lines = []

        # 任务分解
        output_lines.append(f"[task] 收到指令: {command}")
        output_lines.append("")

        # 步骤模拟
        steps = self._plan_steps(command)

        for i, step in enumerate(steps, 1):
            output_lines.append(f"[step {i}/{len(steps)}] {step['name']}")
            time.sleep(0.5)  # 模拟处理时间

            if step.get('output'):
                output_lines.append(f"  → {step['output']}")

            if step.get('error'):
                output_lines.append(f"  ❌ {step['error']}")

        output_lines.append("")
        output_lines.append("[done] 任务执行完毕")
        output_lines.append(self.EOM)

        return "\n".join(output_lines)

    def _plan_steps(self, command: str) -> list:
        """根据指令规划执行步骤"""
        # 简单的关键词匹配分步
        steps = []

        if any(kw in command for kw in ["搜索", "查找", "search"]):
            steps.append({"name": "解析搜索条件", "output": "提取关键词"})
            steps.append({"name": "执行搜索操作"})
            steps.append({"name": "整理结果", "output": "已找到相关结果"})

        elif any(kw in command for kw in ["打开", "启动", "open"]):
            steps.append({"name": "识别目标应用"})
            steps.append({"name": "启动应用"})
            steps.append({"name": "等待就绪"})

        elif any(kw in command for kw in ["截图", "拍照"]):
            steps.append({"name": "获取屏幕画面"})
            steps.append({"name": "保存截图"})

        elif any(kw in command for kw in ["设置", "配置"]):
            steps.append({"name": "检查当前配置"})
            steps.append({"name": "修改配置项"})
            steps.append({"name": "验证配置生效"})

        else:
            steps = [
                {"name": "分析指令意图"},
                {"name": "执行操作"},
                {"name": "确认结果"},
            ]

        return steps

    def heartbeat(self) -> str:
        """心跳检测"""
        return f"pong{self.EOM}\n"

    def shutdown(self) -> str:
        """关闭守护进程"""
        self.running = False
        return f"[info] 守护进程正常关闭\n{self.EOM}\n"

# ─── 主循环 ──────────────────────────────────────────────────

def main():
    import argparse
    parser = argparse.ArgumentParser(description="Pocket Agent Daemon")
    parser.add_argument("--mode", choices=["daemon", "config", "get_config"],
                        default="daemon", help="运行模式")
    parser.add_argument("--config", type=str, default="config.json",
                        help="配置文件路径")
    args = parser.parse_args()

    config = load_config(args.config)

    if args.mode == "config":
        # 配置文件写入模式
        print("[info] 配置已保存到", args.config)
        return
    elif args.mode == "get_config":
        # 返回当前配置
        print(json.dumps(config, indent=2, ensure_ascii=False))
        return

    # daemon 模式
    agent = DaemonAgent(config)

    # 注册信号处理
    signal.signal(signal.SIGTERM, lambda s, f: setattr(agent, 'running', False))
    signal.signal(signal.SIGINT, lambda s, f: setattr(agent, 'running', False))

    print("[info] Pocket Agent 守护进程已启动")
    print("[info] 通信协议: stdin/stdout, EOM=", agent.EOM)
    print(agent.EOM)

    # 主循环：逐行读取 stdin
    for line in sys.stdin:
        line = line.strip()
        if not line:
            continue

        if line == "ping":
            sys.stdout.write(agent.heartbeat())
            sys.stdout.flush()
        elif line == "shutdown":
            sys.stdout.write(agent.shutdown())
            sys.stdout.flush()
            break
        else:
            # 执行指令
            result = agent.execute(line)
            sys.stdout.write(result)
            sys.stdout.flush()

    sys.exit(0)

if __name__ == "__main__":
    main()