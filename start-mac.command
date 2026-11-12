#!/bin/bash
# ============================================================
#  PharmaERP launcher for macOS (runs the standalone jar).
#  Double-click this file (start-mac.command) to run the app.
#  Your browser opens at http://localhost:8081 automatically.
#  Press Control + C in the window to stop the app.
# ============================================================

# Move into the folder this script lives in.
cd "$(dirname "$0")" || exit 1

# Put Java 17 on PATH if JAVA_HOME is not already set.
if [ -z "$JAVA_HOME" ]; then
  if /usr/libexec/java_home -v 17 >/dev/null 2>&1; then
    export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
  fi
fi
[ -n "$JAVA_HOME" ] && export PATH="$JAVA_HOME/bin:$PATH"

if [ ! -f "pharma-erp.jar" ]; then
  echo "ERROR: pharma-erp.jar not found in this folder."
  echo "Build it first with:  ./mvnw -DskipTests clean package"
  echo "then copy target/pharma-erp-1.0.0.jar here as pharma-erp.jar"
  exit 1
fi

echo "Starting PharmaERP..."
echo "The browser will open shortly. Keep this window open while you use the app."

# Open the browser after a short delay (server needs a few seconds to boot).
( sleep 10 && open "http://localhost:8081" ) &

# Run the app from the self-contained jar (no rebuild needed).
java -jar pharma-erp.jar
