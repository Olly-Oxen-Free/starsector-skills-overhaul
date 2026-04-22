#!/bin/bash
# Build script for Skills Overhaul

set -eo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PATCH_DIR="$SCRIPT_DIR"
CLASSES_DIR="$PATCH_DIR/jars/classes"
JAR_FILE="$PATCH_DIR/jars/SkillsOverhaul.jar"
GAME_DIR="/home/jayden-eppcohen/Games/Starsector"

CLASSPATH="$GAME_DIR/starfarer.api.jar:$GAME_DIR/log4j-1.2.9.jar:$GAME_DIR/json.jar:$GAME_DIR/fs.common_obf.jar:$GAME_DIR/lwjgl.jar"

# Handle test target
if [ "$1" = "test" ]; then
    echo "=== Skills Overhaul Test ==="
    echo ""

    # Clean
    echo "[1/3] Cleaning..."
    rm -rf "$CLASSES_DIR"
    mkdir -p "$CLASSES_DIR"

    # Compile src and test
    echo "[2/3] Compiling sources and tests..."
    find "$PATCH_DIR/src" "$PATCH_DIR/test" -name "*.java" -print0 | \
    xargs -0 javac -source 8 -target 8 -cp "$CLASSPATH" -d "$CLASSES_DIR" \
        2>&1 | { grep -v "^Note:" || true; }

    echo "   Compiled successfully"

    # Run tests
    echo "[3/3] Running tests..."
    TEST_CP="$CLASSES_DIR:$CLASSPATH"

    # Find all test classes (ending with *Test)
    cd "$CLASSES_DIR"
    for test_class in $(find . -name "*Test.class" -type f | sed 's|./||;s|/|.|g;s|\.class||'); do
        echo ""
        echo "   Running $test_class..."
        java -ea -cp "$TEST_CP" "$test_class" || exit 1
    done

    echo ""
    echo "✓ ALL TESTS PASSED"
    exit 0
fi

echo "=== Skills Overhaul Build ==="
echo ""

# Clean
echo "[1/3] Cleaning..."
rm -rf "$CLASSES_DIR"
mkdir -p "$CLASSES_DIR"

# Compile
echo "[2/3] Compiling..."

find "$PATCH_DIR/src" -name "*.java" -print0 | \
xargs -0 javac -source 8 -target 8 -cp "$CLASSPATH" -d "$CLASSES_DIR" \
    2>&1 | grep -v "Note:" || true

if [ ! -f "$CLASSES_DIR/skills_overhaul/SkillsOverhaulModPlugin.class" ]; then
    echo "Compilation failed!"
    exit 1
fi

echo "   Compiled successfully"

# Package
echo "[3/3] Creating JAR..."
cd "$CLASSES_DIR"
jar cf "$JAR_FILE" skills_overhaul/

if [ $? -eq 0 ]; then
    echo "   JAR created"
    echo ""
    echo "✓ BUILD SUCCESSFUL"
    ls -lh "$JAR_FILE"
else
    echo "JAR creation failed!"
    exit 1
fi
