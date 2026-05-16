#!/bin/bash

echo "========================================"
echo " HealthBot Stop - $(date '+%Y-%m-%d %H:%M:%S')"
echo "========================================"

pkill -f "healthbot-backend" 2>/dev/null && echo " Backend stopped." || echo " Backend was not running."
pkill -f "uvicorn main:app"  2>/dev/null && echo " LLM service stopped." || echo " LLM service was not running."

echo "========================================"
echo " Stop complete - $(date '+%Y-%m-%d %H:%M:%S')"
echo "========================================"
