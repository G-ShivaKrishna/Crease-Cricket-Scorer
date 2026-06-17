#!/bin/bash
set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

echo "=============================="
echo " CREASE CRICKET SCORER v2.0"
echo " Flutter Edition"
echo "=============================="

# Check Flutter
if ! command -v flutter &> /dev/null; then
    echo "Flutter not found. Please install Flutter SDK."
    exit 1
fi

# Check devices
echo ""
echo "Checking for connected devices..."
DEVICES=$(flutter devices 2>/dev/null | grep -v "^$" | grep -v "^Flutter" | tail -n +2)
echo "$DEVICES"

# Build
echo ""
echo "Building Flutter debug APK..."
flutter build apk --debug 2>&1

# Install and run
echo ""
echo "Installing on device..."
flutter install --debug 2>&1

echo ""
echo "Launching app and streaming logs..."
echo "(Press Ctrl+C to stop)"
flutter run --debug 2>&1
