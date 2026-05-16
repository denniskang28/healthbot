#!/bin/bash
set -e

PROJECT_DIR="/home/healthbot/healthbot"
LOG_DIR="/home/healthbot/logs"
BACKEND_JAR="$PROJECT_DIR/backend/target/healthbot-backend-0.0.1-SNAPSHOT.jar"

mkdir -p "$LOG_DIR"

echo "========================================"
echo " HealthBot Deploy - $(date '+%Y-%m-%d %H:%M:%S')"
echo "========================================"

# ── 1. 拉取最新代码 ──────────────────────────────
echo "[1/5] Pulling latest code..."
cd "$PROJECT_DIR"
git pull origin main
echo "      Done."

# ── 2. Build Backend ─────────────────────────────
echo "[2/5] Building backend..."
cd "$PROJECT_DIR/backend"
./mvnw package -DskipTests -q
echo "      Done: $BACKEND_JAR"

# ── 3. Build Admin ───────────────────────────────
echo "[3/5] Building admin..."
cd "$PROJECT_DIR/admin"
npm install --silent
npm run build --silent
echo "      Done: admin/dist/"

# ── 4. 停止旧服务 ────────────────────────────────
echo "[4/5] Stopping old services..."

pkill -f "healthbot-backend" 2>/dev/null && echo "      Backend stopped." || echo "      Backend was not running."
pkill -f "uvicorn main:app"  2>/dev/null && echo "      LLM service stopped." || echo "      LLM service was not running."
sleep 2

# ── 5. 启动新服务 ────────────────────────────────
echo "[5/5] Starting services..."

# LLM Service
cd "$PROJECT_DIR/llm-service"
source venv/bin/activate
nohup uvicorn main:app --host 127.0.0.1 --port 8000 > "$LOG_DIR/llm.log" 2>&1 &
echo "      LLM service started (pid $!)."

# Backend — cd first so H2 ./data/ resolves to backend/data/ consistently
cd "$PROJECT_DIR/backend"
sleep 2
nohup java -jar "$BACKEND_JAR" > "$LOG_DIR/backend.log" 2>&1 &
echo "      Backend started (pid $!)."

# Nginx（只重载配置，不重启进程）
sudo systemctl reload nginx
echo "      Nginx reloaded."

echo ""
echo "========================================"
echo " Deploy complete - $(date '+%Y-%m-%d %H:%M:%S')"
echo " Logs: $LOG_DIR/backend.log"
echo "       $LOG_DIR/llm.log"
echo "========================================"
