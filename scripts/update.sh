#!/data/data/com.termux/files/usr/bin/bash
# ============================================================
# Pocket-Agent Update Script
# ============================================================
# Pulls the latest agent code from GitHub and updates deps.
# Can be called manually from the terminal or automatically
# on app startup via GitUpdater.kt.
# ============================================================

set -e

AGENT_DIR="/data/data/com.termux/files/home/pocket-agent"
GIT_REMOTE="https://github.com/Dreamt0511/Pocket-Agent.git"

echo "[Pocket-Agent Update] Checking for updates..."

cd "$AGENT_DIR"

# Stash any local changes
git stash --include-untracked 2>/dev/null || true

# Pull latest
if git pull origin main 2>&1; then
    echo "[Pocket-Agent Update] Code updated successfully."
else
    echo "[Pocket-Agent Update] Pull failed, trying reset..."
    git fetch origin main
    git reset --hard origin/main
fi

# Update dependencies
if [ -f "requirements.txt" ]; then
    echo "[Pocket-Agent Update] Updating dependencies..."
    pip install -r requirements.txt -q
fi

# Show current version
COMMIT=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
echo "[Pocket-Agent Update] Current commit: $COMMIT"
echo "[Pocket-Agent Update] Done."