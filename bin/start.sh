#!/bin/bash
set -e

PROJECT_DIR="/home/healthbot/healthbot"
LOG_DIR="/home/healthbot/logs"
BACKEND_JAR="$PROJECT_DIR/backend/target/healthbot-backend-0.0.1-SNAPSHOT.jar"

mkdir -p "$LOG_DIR"

echo "========================================"
echo " HealthBot Start - $(date '+%Y-%m-%d %H:%M:%S')"
echo "========================================"

# ── LLM Service ──────────────────────────────────
echo "[1/2] Starting LLM service..."
cd "$PROJECT_DIR/llm-service"
source venv/bin/activate
nohup uvicorn main:app --host 127.0.0.1 --port 8000 > "$LOG_DIR/llm.log" 2>&1 &
echo "      LLM service started (pid $!)."

# ── Backend ──────────────────────────────────────
echo "[2/2] Starting backend..."
cd "$PROJECT_DIR/backend"
sleep 2
nohup java -jar "$BACKEND_JAR" > "$LOG_DIR/backend.log" 2>&1 &
echo "      Backend started (pid $!)."

echo ""
echo "========================================"
echo " Start complete - $(date '+%Y-%m-%d %H:%M:%S')"
echo " Logs: $LOG_DIR/backend.log"
echo "       $LOG_DIR/llm.log"
echo "========================================"
