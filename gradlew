#!/bin/sh
set -eu
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
export GRADLE_USER_HOME="$PROJECT_DIR/.gradle-user-home"
if ! command -v gradle >/dev/null 2>&1; then
  echo 'Requires an existing Gradle 9.1+ and JDK 25. No toolchain will be installed.' >&2
  exit 1
fi
exec gradle -p "$PROJECT_DIR" "$@"