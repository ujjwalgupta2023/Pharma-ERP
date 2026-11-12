#!/bin/bash
# ============================================================
#  Builds a native macOS installer (.dmg) for PharmaERP using
#  jpackage. The resulting app bundles its own Java runtime, so
#  the end user installs it like any Mac app and gets a launchpad
#  icon — no Java install required.
#
#  Run this ON A MAC (jpackage cannot cross-build). Requires JDK 17.
# ============================================================
set -e
cd "$(dirname "$0")/.."

if [ -z "$JAVA_HOME" ]; then
  if /usr/libexec/java_home -v 17 >/dev/null 2>&1; then
    export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
  fi
fi
JP="$JAVA_HOME/bin/jpackage"

echo "[1/3] Building application jar..."
chmod +x ./mvnw
./mvnw -q -DskipTests clean package

echo "[2/3] Staging jar..."
rm -rf build-input && mkdir build-input
cp target/pharma-erp-1.0.0.jar build-input/

echo "[3/3] Running jpackage (.dmg)..."
rm -rf dist && mkdir dist
"$JP" \
  --type dmg \
  --name PharmaERP \
  --input build-input \
  --main-jar pharma-erp-1.0.0.jar \
  --main-class org.springframework.boot.loader.launch.JarLauncher \
  --dest dist \
  --app-version 1.0.0 \
  --vendor "PharmaERP" \
  --java-options "-Xmx512m"

echo ""
echo "DONE. Installer is in dist/ : dist/PharmaERP-1.0.0.dmg"
echo "Open the .dmg and drag PharmaERP to Applications."
