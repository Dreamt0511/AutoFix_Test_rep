"""
Pocket-Agent Task Executor

Translates the plan (from TaskPlanner) into actual phone operations
via the MCP client. Handles retry logic and error recovery.
"""

from agent.mcp_client import MCPClient
from agent.planner import TaskPlanner
from typing import Callable


class TaskExecutor:
    """
    Executes a planned sequence of phone operations.
    """

    # Map of app names to Android package names
    APP_PACKAGES = {
        "抖音": "com.ss.android.ugc.aweme",
        "咸鱼": "com.taobao.idlefish",
        "闲鱼": "com.taobao.idlefish",
        "微信": "com.tencent.mm",
        "淘宝": "com.taobao.taobao",
        "京东": "com.jingdong.app.mall",
        "小红书": "com.xingin.xhs",
        "设置": "com.android.settings",
    }

    def __init__(self, mcp_client: MCPClient, planner: TaskPlanner):
        self.mcp = mcp_client
        self.planner = planner
        self.max_retries = 3

    def execute(self, task: str, callback: Callable[[str, str], None]) -> bool:
        """
        Execute a natural language task.

        Returns True if all steps completed successfully.
        """
        # Plan
        callback("planning", "正在规划操作步骤...")
        plan = self.planner.plan(task)

        if not self.mcp.connect():
            callback("error", "无法连接到 MCP 服务，请确认无障碍服务已开启")
            return False

        try:
            for i, step in enumerate(plan, 1):
                action = step.get("action", "")
                description = step.get("description", f"步骤 {i}")

                callback("executing", f"({i}/{len(plan)}) {description}")

                success = False
                for attempt in range(self.max_retries):
                    success = self._do_action(step)
                    if success:
                        break
                    callback("executing",
                        f"({i}/{len(plan)}) 重试 {attempt + 1}/{self.max_retries}...")

                if not success:
                    callback("error", f"步骤失败: {description}")
                    return False

            return True
        finally:
            self.mcp.disconnect()

    def _do_action(self, step: dict) -> bool:
        """Execute a single action from the plan."""
        action = step.get("action", "")
        params = step.get("params", {})

        handlers = {
            "tap": self._handle_tap,
            "swipe": self._handle_swipe,
            "type": self._handle_type,
            "open_app": self._handle_open_app,
            "search": self._handle_search,
            "wait": self._handle_wait,
            "press_back": lambda p: self.mcp.press_back(),
            "press_home": lambda p: self.mcp.press_home(),
            "report": lambda p: True,
        }

        handler = handlers.get(action)
        if handler:
            return handler(params)
        return False

    def _handle_tap(self, params: dict) -> bool:
        if "x" in params and "y" in params:
            return self.mcp.tap(params["x"], params["y"])
        if "element" in params:
            el = self.mcp.find_element(params["element"])
            if el:
                return self.mcp.tap(el["x"], el["y"])
        return False

    def _handle_swipe(self, params: dict) -> bool:
        return self.mcp.swipe(
            params.get("x1", 0),
            params.get("y1", 0),
            params.get("x2", 0),
            params.get("y2", 0),
            params.get("duration", 300),
        )

    def _handle_type(self, params: dict) -> bool:
        return self.mcp.type_text(params.get("text", ""))

    def _handle_open_app(self, params: dict) -> bool:
        app_name = params.get("app_name", "")
        package = self.APP_PACKAGES.get(app_name, app_name)
        return self.mcp.open_app(package)

    def _handle_search(self, params: dict) -> bool:
        query = params.get("query", "")
        if query:
            return self.mcp.type_text(query)
        return False

    def _handle_wait(self, params: dict) -> bool:
        return self.mcp.wait(params.get("seconds", 1.0))