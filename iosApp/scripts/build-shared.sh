#!/bin/bash
set -euo pipefail
if [[ "${SKIP_KOTLIN_BUILD:-NO}" == "YES" ]]; then exit 0; fi
REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$REPO_ROOT"
bash ./gradlew :shared:assembleSharedKitReleaseXCFramework
