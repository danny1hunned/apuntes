#!/bin/bash
set -euo pipefail
REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
if [[ "$(uname -s)" != "Darwin" ]]; then
    printf 'Preparing the iOS project requires macOS and Xcode.\n' >&2
    exit 1
fi
xcode_major="$(xcodebuild -version | awk '/^Xcode / {split($2, parts, "."); print parts[1]}')"
sdk_major="$(xcrun --sdk iphoneos --show-sdk-version | cut -d. -f1)"
if [[ "$xcode_major" -lt 26 || "$sdk_major" -lt 26 ]]; then
    printf 'App Store builds require Xcode 26+ and the iOS 26+ SDK. Select Xcode using DEVELOPER_DIR or xcode-select.\n' >&2
    exit 1
fi
command -v xcodegen >/dev/null || { printf 'Install XcodeGen with: brew install xcodegen\n' >&2; exit 1; }
command -v dwebp >/dev/null || { printf 'Install the icon decoder with: brew install webp\n' >&2; exit 1; }
cd "$REPO_ROOT"
SDK_PATH="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
if [[ ! -d "$SDK_PATH" ]]; then
    printf 'Set ANDROID_HOME to the installed Android SDK directory.\n' >&2
    exit 1
fi
# local.properties is tracked with a Windows SDK path in this repository.
python3 iosApp/scripts/configure-sdk.py "$SDK_PATH"
bash iosApp/scripts/build-shared.sh
mkdir -p iosApp/build
dwebp app/src/main/res/mipmap-xxxhdpi/app_icon.webp -o iosApp/build/source-icon.png
xcrun swift iosApp/scripts/PrepareIcon.swift "$REPO_ROOT"
cd iosApp
xcodegen generate
