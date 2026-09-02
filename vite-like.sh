#!/bin/bash
# vite-like for StreetComplete without Android Studio
# Watches Kotlin/Compose/XML and does incremental installDebug (like Vite HMR)
# Preserves /data (no uninstall, no wipe-data) — unlike fresh install
set -e

# --- config ---
export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ADB="$ANDROID_HOME/platform-tools/adb"
EMULATOR="$ANDROID_HOME/emulator/emulator"
AVD="Small_Phone"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=== StreetComplete vite-like (no AS) ==="
echo "JAVA_HOME=$JAVA_HOME"
java -version 2>&1 | head -n 1
echo "ANDROID_HOME=$ANDROID_HOME"
echo "ADB=$ADB"
$ADB devices -l | head -n 10

# ensure emulator
if ! $ADB devices | grep -q "emulator-5554"; then
  echo "Starting emulator $AVD..."
  nohup $EMULATOR -avd $AVD > /tmp/emulator-vite.log 2>&1 &
  echo "Waiting for boot..."
  for i in {1..30}; do
    BOOT=$($ADB -s emulator-5554 shell getprop sys.boot_completed 2>&1 | tr -d '\r')
    if [ "$BOOT" = "1" ]; then echo "Booted"; break; fi
    sleep 2
  done
fi

# verify boot
BOOT=$($ADB -s emulator-5554 shell getprop sys.boot_completed 2>&1 | tr -d '\r' || echo "0")
if [ "$BOOT" != "1" ]; then
  echo "Emulator not ready (boot=$BOOT). Run: adb devices"
  exit 1
fi

echo "Emulator ready: $($ADB -s emulator-5554 shell getprop sys.boot_completed | tr -d '\r')"
$ADB -s emulator-5554 shell df -h | head -n 5

# initial build+install (warm daemon, ~2-3m after first 23m)
echo ""
echo ">>> Initial installDebug (warm)..."
cd "$PROJECT_DIR"
# stop old daemons on wrong JDK
./gradlew --stop 2>&1 | tail -n 5 || true
# --continuous is Vite-like, but we do one-shot first to warm
./gradlew :androidApp:installDebug 2>&1 | tail -n 30

echo ""
echo "=== App installed as de.westnordost.streetcomplete.debug ==="
echo "Launch: adb -s emulator-5554 shell am start -n de.westnordost.streetcomplete.debug/de.westnordost.streetcomplete.screens.main.MainActivity"
$ADB -s emulator-5554 shell am start -n de.westnordost.streetcomplete.debug/de.westnordost.streetcomplete.screens.main.MainActivity 2>&1 | tail -n 5

# --- watch mode ---
if command -v fswatch >/dev/null 2>&1; then
  echo ""
  echo "=== Watching for changes (like vite --watch) ==="
  echo "Watching: app/src, androidApp/src (fswatch)"
  echo "Edit any .kt/.xml -> auto rebuild + adb install -r (preserves data)"
  echo "Ctrl+C to stop"
  echo ""
  # batch events 1s, ignore build/ and .gradle
  fswatch -0 -r -e "build" -e ".gradle" -e ".git" --event Created --event Updated --event Removed \
    "$PROJECT_DIR/app/src" "$PROJECT_DIR/androidApp/src" 2>/dev/null | while read -d "" event; do
      echo ""
      echo ">>> Changed: $event at $(date +%H:%M:%S)"
      # debounce 1s
      sleep 1
      echo ">>> Rebuilding incremental..."
      if ./gradlew :androidApp:installDebug 2>&1 | tail -n 20; then
        echo ">>> Reinstalled, relaunching..."
        $ADB -s emulator-5554 shell am start -n de.westnordost.streetcomplete.debug/de.westnordost.streetcomplete.screens.main.MainActivity 2>&1 | tail -n 3
      else
        echo ">>> Build failed — fix and save again"
      fi
  done
else
  echo ""
  echo "fswatch not found. Fallback to Gradle --continuous (also Vite-like):"
  echo "  JAVA_HOME=\$JAVA_HOME ./gradlew :androidApp:installDebug --continuous"
  echo "Then edit files — Gradle watches task inputs and auto-rebuilds + installs."
  echo ""
  echo "Starting --continuous now (Ctrl+C to stop)..."
  ./gradlew :androidApp:installDebug --continuous
fi
