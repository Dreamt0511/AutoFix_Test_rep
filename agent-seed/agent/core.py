"""
Pocket-Agent Core Engine

The main agent loop:
1. Receive a natural language task
2. Plan the sequence of phone operations needed
3. Execute each operation via MCP client
4. Report progress via callback
"""

import json
from typing import Callable, Optional


class AgentCore:
    """
    The core agent that orchestrates task planning and execution.

    In the full implementation, this uses an LLM (via the user's API key)
    to understand the task, decompose it into phone operations, and then
    execute them via the MCP server running on the phone.

    For the MVP seed, this provides a structured skeleton that the real
    agent code (pulled from GitHub) will replace.
    """

    def __init__(self, config: dict):
        self.config = config
        self.api_base = config.get("api_base_url", "https://api.openai.com/v1")
        self.model = config.get("model_name", "gpt-4o")
        self.api_key = config.get("api_key", "")

    def execute(self, task: str, callback: Callable[[str, str], None]):
        """
        Execute a task and report progress via callback.

        Args:
            task: The natural language task description
            callback: Function(status, message) called for each step
        """
        callback("planning", "正在分析任务...")
        plan = self._plan(task)

        for i, step in enumerate(plan, 1):
            callback("executing", f"步骤 {i}/{len(plan)}: {step}")
            success = self._execute_step(step)
            if not success:
                callback("error", f"步骤失败: {step}")
                return

        callback("done", f"任务完成，共执行 {len(plan)} 个步骤")

    def _plan(self, task: str) -> list:
        """
        Decompose a natural language task into a sequence of phone operations.

        In the full implementation, this calls the LLM to generate the plan.
        For the seed MVP, it returns a placeholder plan.

        The plan format is a list of operation descriptions that the MCP
        client can translate into AccessibilityService actions.
        """
        # Placeholder: in the real agent (from GitHub), this uses the LLM
        return [
            "打开目标应用",
            "搜索目标内容",
            "执行目标操作",
            "收集结果",
        ]

    def _execute_step(self, step: str) -> bool:
        """
        Execute a single planned step via the MCP client.

        Returns True on success, False on failure.
        """
        # Placeholder: in the real agent, this calls the MCP server
        return True