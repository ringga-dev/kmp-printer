#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"

if ! command -v swift >/dev/null 2>&1; then
  echo "Swift was not found."
  echo "Install Xcode Command Line Tools with:"
  echo "  xcode-select --install"
  exit 1
fi

if ! xcode-select -p >/dev/null 2>&1; then
  echo "Xcode Command Line Tools are not configured."
  echo "Starting installer..."
  xcode-select --install || true
  echo "Run this script again after installation finishes."
  exit 1
fi

cd "$ROOT_DIR"
swift build -c release

OUTPUT="$ROOT_DIR/.build/release/kmp-printer-ble-macos"
echo ""
echo "BLE helper built:"
echo "$OUTPUT"
echo ""
echo "Use this path in PrinterConfig.bleBridgeCommand or add it to PATH."
