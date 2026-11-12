#!/usr/bin/env bash
set -e

# AIAgent Terminal - Release Build Script
# This script builds a signed Release APK using my-upload-key.jks

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

echo "=============================================="
echo " AIAgent Terminal - Building Signed Release APK"
echo "=============================================="

export KEYSTORE_PATH="${ROOT_DIR}/my-upload-key.jks"
export STORE_PASSWORD="android"
export KEY_PASSWORD="android"

if [ ! -f "$KEYSTORE_PATH" ]; then
    echo "❌ Error: Keystore not found at $KEYSTORE_PATH"
    exit 1
fi

echo "✅ Keystore found: $KEYSTORE_PATH (Alias: upload)"
echo "🚀 Running gradle assembleRelease..."

gradle :app:assembleRelease

echo "=============================================="
echo "🎉 Build completed successfully!"
echo "Signed APK location: app/build/outputs/apk/release/app-release.apk"
echo "=============================================="
