#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
APK="${1:-$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk}"
BUILD_TOOLS="$SDK_ROOT/build-tools/36.0.0"

if [[ ! -f "$APK" ]]; then
  echo "APK not found: $APK" >&2
  exit 1
fi

"$BUILD_TOOLS/apksigner" verify --verbose --print-certs "$APK"
"$BUILD_TOOLS/zipalign" -c -p 4 "$APK"
"$BUILD_TOOLS/aapt2" dump badging "$APK" | grep -E \
  "^(package:|minSdkVersion:|targetSdkVersion:|application-label:|launchable-activity:)"

if "$BUILD_TOOLS/aapt2" dump permissions "$APK" | grep -q 'android.permission.INTERNET'; then
  echo "Unexpected INTERNET permission present." >&2
  exit 1
fi

if unzip -Z1 "$APK" | grep -Eq '(^|/)(initial_history\.puud|.*\.(jks|keystore))$'; then
  echo "Private data or signing material found in APK." >&2
  exit 1
fi

echo "APK verification passed; no Internet permission or private seed data."
