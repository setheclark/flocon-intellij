#!/bin/bash
# Build script for Flocon IntelliJ Plugin
# This wrapper ensures the Flocon submodule compiles with Java 21 bytecode
# for IntelliJ Platform compatibility.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INIT_SCRIPT="$SCRIPT_DIR/gradle/flocon-jvm21.init.gradle"

exec "$SCRIPT_DIR/gradlew" --init-script "$INIT_SCRIPT" "$@"
