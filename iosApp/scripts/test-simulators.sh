#!/bin/bash
set -euo pipefail
REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$REPO_ROOT"
mkdir -p iosApp/build
for family in iPhone iPad; do
    device_id="$(xcrun simctl list devices available --json | python3 -c '
import json, sys
family = sys.argv[1]
devices = json.load(sys.stdin)["devices"]
matches = [d for runtime, items in devices.items() if "iOS" in runtime for d in items if d["name"].startswith(family) and d.get("isAvailable")]
if not matches:
    sys.exit("No available " + family + " simulator. Install an iOS simulator runtime in Xcode.")
print(matches[0]["udid"])
' "$family")"
    xcodebuild test -project iosApp/Apuntes.xcodeproj -scheme Apuntes \
        -destination "platform=iOS Simulator,id=$device_id" \
        -parallel-testing-enabled NO \
        -derivedDataPath iosApp/build/DerivedData \
        -resultBundlePath "iosApp/build/$family-$(date +%s).xcresult" \
        CODE_SIGNING_ALLOWED=NO
done
