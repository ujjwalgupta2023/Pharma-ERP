@echo off
REM ============================================================
REM  PharmaERP launcher for Windows (runs the standalone jar).
REM  Double-click this file to start the app. Your browser opens
REM  at http://localhost:8081 automatically after a few seconds.
REM  Close this window (or press Ctrl+C) to stop the app.
REM ============================================================

cd /d "%~dp0"

REM --- Locate Java if JAVA_HOME is not set (so java is on PATH) ---
if not defined JAVA_HOME (
    if exist "C:\Program Files\Java\jdk-17" set "JAVA_HOME=C:\Program Files\Java\jdk-17"
)
if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"

if not exist "pharma-erp.jar" (
    echo ERROR: pharma-erp.jar not found in this folder.
    echo Build it first with:  mvnw.cmd -DskipTests clean package
    echo then copy target\pharma-erp-1.0.0.jar here as pharma-erp.jar
    pause
    exit /b 1
)

echo Starting PharmaERP...
echo The browser will open shortly. Keep this window open while you use the app.

REM --- Open the browser after a short delay (server needs a few seconds) ---
start "" cmd /c "timeout /t 10 >nul & start http://localhost:8081"

REM --- Run the app from the self-contained jar (no rebuild needed) ---
java -jar pharma-erp.jar

pause
