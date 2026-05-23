"""
Pocket-Agent Task Planner

Uses LLM to decompose natural language tasks into executable
phone operation sequences.
"""

from typing import Optional


class TaskPlanner:
    """
    Plans task execution by calling the LLM to break down a
    natural language instruction into concrete phone operations.
    """

    def __init__(self, api_base: str, api_key: str, model: str):
        self.api_base = api_base
        self.api_key = api_key
        self.model = model

    def plan(self, task: str) -> list[dict]:
        """
        Generate a plan from a natural language task.

        Returns a list of operations, each with:
        - action: str (tap, swipe, type, wait, screenshot, etc.)
        - target: str (what to interact with)
        - params: dict (action-specific parameters)

        In the full implementation, this calls the OpenAI-compatible API.
        """
        # Placeholder plan for MVP seed
        return [
            {
                "action": "open_app",
                "target": "待解析",
                "params": {},
                "description": "打开目标应用"
            },
            {
                "action": "search",
                "target": "待解析",
                "params": {"query": "由 LLM 提取"},
                "description": "搜索目标内容"
            },
            {
                "action": "tap",
                "target": "待解析",
                "params": {"element": "由 LLM 识别"},
                "description": "点击目标元素"
            },
            {
                "action": "report",
                "target": "",
                "params": {},
                "description": "汇总结果"
            },
        ]