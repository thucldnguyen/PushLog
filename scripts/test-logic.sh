#!/usr/bin/env bash
set -euo pipefail
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$PROJECT_DIR/app/build/logic-tests"
mkdir -p "$OUT"
SOURCES="$PROJECT_DIR/app/src/main/java/com/thuc/pushlog"
TESTS="$PROJECT_DIR/app/src/test/java/com/thuc/pushlog"
javac --release 17 -d "$OUT" "$SOURCES/ChartAxis.java" "$SOURCES/DailyReminderLogic.java" "$TESTS/ChartAxisTest.java" "$TESTS/DailyReminderLogicTest.java"
java -cp "$OUT" com.thuc.pushlog.ChartAxisTest
java -cp "$OUT" com.thuc.pushlog.DailyReminderLogicTest
printf '%s\n' 'Chart axis and reminder logic tests passed.'
