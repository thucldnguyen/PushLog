#!/usr/bin/env bash
set -euo pipefail
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_DIR"
./gradlew assembleDebug
printf '%s\n' "Built app/build/outputs/apk/debug/app-debug.apk (development signing only)."
