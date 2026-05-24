#!/data/data/com.termux/files/usr/bin/bash
# ============================================================
# Pocket-Agent Termux Bootstrap Script (v2)
# ============================================================
# 在 Termux 环境中设置 Python 运行时和 Agent 依赖
# 由 TermuxBootstrap.kt 在应用初始化时调用
# ============================================================

set -e

echo "[Pocket-Agent Bootstrap] Starting..."

# Environment
export HOME=/data/data/com.termux/files/home
export PREFIX=/data/data/com.termux/files/usr
export PATH=$PREFIX/bin:$PREFIX/bin/applets:$PATH
export LD_LIBRARY_PATH=$PREFIX/lib:$LD_LIBRARY_PATH

# Update package lists (with timeout)
echo "[Pocket-Agent Bootstrap] Updating packages..."
apt-get update -y -q || echo "Warning: apt-get update failed, continuing..."

# Install core dependencies
echo "[Pocket-Agent Bootstrap] Installing Python and tools..."
apt-get install -y -q python python-pip git curl openssh || {
    echo "[Pocket-Agent Bootstrap] Package installation failed"
    exit 1
}

# Verify installations
echo "[Pocket-Agent Bootstrap] Verifying..."
python3 --version
pip --version
git --version

# Install pip upgrades
echo "[Pocket-Agent Bootstrap] Upgrading pip..."
pip install --upgrade pip -q 2>/dev/null || true

# Clone/Update agent repository
AGENT_DIR=/data/data/com.termux/files/home/pocket-agent
if [ -d "$AGENT_DIR/.git" ]; then
    echo "[Pocket-Agent Bootstrap] Updating agent repository..."
    cd $AGENT_DIR
    git stash -q 2>/dev/null || true
    git pull origin main -q || echo "Warning: git pull failed"
else
    echo "[Pocket-Agent Bootstrap] Cloning agent repository..."
    git clone https://github.com/Dreamt0511/Pocket-Agent.git $AGENT_DIR || {
        echo "[Pocket-Agent Bootstrap] Clone failed, using seed code"
    }
fi

# Install Python dependencies
if [ -f "$AGENT_DIR/requirements.txt" ]; then
    echo "[Pocket-Agent Bootstrap] Installing Python dependencies..."
    cd $AGENT_DIR
    pip install -r requirements.txt -q 2>/dev/null || {
        echo "[Pocket-Agent Bootstrap] Warning: Some dependencies failed to install"
        # Try installing core dependencies individually
        pip install python-dotenv nest-asyncio aiohttp pillow rich requests -q 2>/dev/null || true
    }
fi

# Create directories
mkdir -p $AGENT_DIR/tasks $AGENT_DIR/logs $AGENT_DIR/memory 2>/dev/null || true

echo "[Pocket-Agent Bootstrap] Complete!"
echo "Agent directory: $AGENT_DIR"
echo "Run: cd $AGENT_DIR && python3 stable_entry.py"