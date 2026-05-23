#!/usr/bin/env python3
"""
Pocket-Agent Stable Entry Point

This is the IMMUTABLE interface between the Android APK and the Python agent.
The APK calls this script; it in turn loads the latest agent code from the
repository. No matter how the agent internals change, this file stays the same.

Interface contract:
- Called as: python3 stable_entry.py [--mode=task]
- In task mode, reads task description from stdin (JSON line)
- Outputs JSON lines to stdout: {"type": "step", "status": "...", "message": "..."}
- Exit code 0 on success, non-zero on failure
"""

import json
import os
import sys
import time
from pathlib import Path

# Agent code lives alongside this file
AGENT_ROOT = Path(__file__).parent
AGENT_DIR = AGENT_ROOT / "agent"
TASKS_DIR = AGENT_ROOT / "tasks"

# Ensure agent and tasks directories are on the path
sys.path.insert(0, str(AGENT_ROOT))
sys.path.insert(0, str(AGENT_DIR))
sys.path.insert(0, str(TASKS_DIR))


def log_step(status: str, message: str):
    """Emit a step update as a JSON line to stdout."""
    step = {
        "type": "step",
        "status": status,
        "message": message,
        "timestamp": int(time.time())
    }
    print(json.dumps(step, ensure_ascii=False), flush=True)


def load_config() -> dict:
    """Load agent configuration."""
    config_path = AGENT_ROOT / "config.json"
    if config_path.exists():
        with open(config_path, "r") as f:
            return json.load(f)
    return {}


def run_task_mode(task: str):
    """Execute a single task and report progress."""
    log_step("planning", f"收到任务: {task}")

    # Try to import the real agent core
    try:
        from agent.core import AgentCore
        agent = AgentCore(config=load_config())
        log_step("executing", "Agent 引擎已启动")

        # Execute the task with step-by-step callbacks
        agent.execute(task, callback=lambda status, msg: log_step(status, msg))

        log_step("done", "任务执行完毕")
    except ImportError as e:
        log_step("error", f"Agent 核心模块加载失败: {e}")
        log_step("error", "请确保 agent/ 目录已从 GitHub 仓库更新")
        sys.exit(1)
    except Exception as e:
        log_step("error", f"任务执行失败: {e}")
        sys.exit(1)


def run_ready_mode():
    """Report agent status and exit."""
    status = {
        "type": "ready",
        "agent_root": str(AGENT_ROOT),
        "agent_available": (AGENT_DIR / "core.py").exists(),
        "tasks_available": TASKS_DIR.exists(),
        "timestamp": int(time.time())
    }
    print(json.dumps(status, ensure_ascii=False), flush=True)


def main():
    mode = "--mode=task" in sys.argv

    if mode:
        # Read task from stdin
        task_input = sys.stdin.read().strip()
        if not task_input:
            log_step("error", "未收到任务描述")
            sys.exit(1)
        run_task_mode(task_input)
    else:
        run_ready_mode()


if __name__ == "__main__":
    main()