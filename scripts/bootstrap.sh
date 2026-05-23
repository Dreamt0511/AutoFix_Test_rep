#!/data/data/com.termux/files/usr/bin/bash
# ============================================================
# Pocket-Agent Termux Bootstrap Script
# ============================================================
# This script runs inside the Termux environment to set up
# the Python runtime and agent dependencies.
#
# Called by TermuxBootstrap.kt during app initialization.
# ============================================================

set -e

echo "[Pocket-Agent Bootstrap] Starting..."

# Environment
export HOME=/data/data/com.termux/files/home
export PREFIX=/data/data/com.termux/files/usr
export PATH=$PREFIX/bin:$PREFIX/bin/applets:$PATH
export LD_LIBRARY_PATH=$PREFIX/lib:$LD_LIBRARY_PATH

# Update package lists
echo "[Pocket-Agent Bootstrap] Updating packages..."
apt-get update -y -q

# Install core dependencies
echo "[Pocket-Agent Bootstrap] Installing Python..."
apt-get install -y -q python python-pip git curl

# Verify installations
echo "[Pocket-Agent Bootstrap] Verifying..."
python3 --version
pip --version
git --version

# Clone agent repository
AGENT_DIR=/data/data/com.termux/files/home/pocket-agent
if [ -d "$AGENT_DIR/.git" ]; then
    echo "[Pocket-Agent Bootstrap] Updating agent..."
    cd $AGENT_DIR && git pull origin main
else
    echo "[Pocket-Agent Bootstrap] Cloning agent..."
    git clone https://github.com/Dreamt0511/Pocket-Agent.git $AGENT_DIR
fi

# Install Python dependencies
echo "[Pocket-Agent Bootstrap] Installing Python dependencies..."
cd $AGENT_DIR && pip install -r requirements.txt -q

echo "[Pocket-Agent Bootstrap] Complete!"
echo "Agent directory: $AGENT_DIR"
echo "Run: cd $AGENT_DIR && python3 stable_entry.py"