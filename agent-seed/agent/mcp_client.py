"""
Pocket-Agent MCP Client

Communicates with the local MCP server app that provides
AccessibilityService-based phone control capabilities.

Protocol: JSON over a local TCP socket or stdin/stdout.
The MCP server exposes actions like:
- tap(x, y)
- swipe(x1, y1, x2, y2, duration)
- type(text)
- screenshot() -> base64
- find_element(description) -> (x, y, bounds)
- open_app(package_name)
- press_key(keycode)
"""

import json
import socket
import time
from typing import Optional


class MCPClient:
    """
    Client for the local MCP server that controls the phone via AccessibilityService.

    The MCP server is a separate Android app that must be installed and
    have AccessibilityService enabled. It listens on localhost for commands.
    """

    DEFAULT_HOST = "127.0.0.1"
    DEFAULT_PORT = 9876

    def __init__(self, host: str = DEFAULT_HOST, port: int = DEFAULT_PORT):
        self.host = host
        self.port = port
        self.socket: Optional[socket.socket] = None

    def connect(self) -> bool:
        """Connect to the MCP server. Returns True on success."""
        try:
            self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.socket.settimeout(5)
            self.socket.connect((self.host, self.port))
            return True
        except Exception as e:
            print(f"MCP connection failed: {e}")
            return False

    def disconnect(self):
        """Close connection to MCP server."""
        if self.socket:
            try:
                self.socket.close()
            except Exception:
                pass
            self.socket = None

    def send_command(self, action: str, **params) -> dict:
        """Send a command to the MCP server and return the result."""
        if not self.socket:
            return {"success": False, "error": "Not connected"}

        command = {"action": action, "params": params}
        try:
            self.socket.sendall((json.dumps(command) + "\n").encode())
            response = self.socket.recv(4096).decode()
            return json.loads(response)
        except Exception as e:
            return {"success": False, "error": str(e)}

    # --- High-level operations ---

    def tap(self, x: int, y: int) -> bool:
        """Tap at coordinates."""
        result = self.send_command("tap", x=x, y=y)
        return result.get("success", False)

    def swipe(self, x1: int, y1: int, x2: int, y2: int, duration: int = 300) -> bool:
        """Swipe from (x1, y1) to (x2, y2)."""
        result = self.send_command("swipe", x1=x1, y1=y1, x2=x2, y2=y2, duration=duration)
        return result.get("success", False)

    def type_text(self, text: str) -> bool:
        """Type text into the currently focused field."""
        result = self.send_command("type", text=text)
        return result.get("success", False)

    def open_app(self, package_name: str) -> bool:
        """Open an app by package name."""
        result = self.send_command("open_app", package_name=package_name)
        return result.get("success", False)

    def find_element(self, text: str, timeout: float = 5.0) -> Optional[dict]:
        """Find a UI element containing text. Returns {x, y, bounds} or None."""
        start = time.time()
        while time.time() - start < timeout:
            result = self.send_command("find_element", text=text)
            if result.get("success"):
                return result.get("element")
            time.sleep(0.5)
        return None

    def screenshot(self) -> Optional[str]:
        """Take a screenshot, returns base64-encoded image."""
        result = self.send_command("screenshot")
        if result.get("success"):
            return result.get("image")
        return None

    def press_back(self) -> bool:
        """Press the back button."""
        result = self.send_command("press_key", keycode=4)
        return result.get("success", False)

    def press_home(self) -> bool:
        """Press the home button."""
        result = self.send_command("press_key", keycode=3)
        return result.get("success", False)

    def wait(self, seconds: float) -> bool:
        """Wait for a specified duration."""
        time.sleep(seconds)
        return True