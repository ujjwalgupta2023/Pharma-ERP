@echo off
REM ============================================================
REM  Builds a native Windows package for PharmaERP using jpackage.
REM  - Always builds a portable app-image (no Java needed by user).
REM  - If WiX Toolset v3 is installed, also builds an .exe installer
REM    with Desktop + Start Menu shortcuts (like a normal app).
REM
REM  Requires: JDK 17 (for jpackage). WiX is optional:
REM    winget install WiXToolset.WiX
REM    or https://github.com/wixtoolset/wix3/releases
REM ============================================================

setlocal
cd /d "%~dp0.."

if not defined JAVA_HOME (
    if exist "C:\Program Files\Java\jdk-17" set "JAVA_HOME=C:\Program Files\Java\jdk-17"
)
set "JP=%JAVA_HOME%\bin\jpackage.exe"

echo [1/3] Building application jar...
call mvnw.cmd -q -DskipTests clean package || goto :error

echo [2/3] Staging jar...
if exist build-input rmdir /s /q build-input
mkdir build-input
copy /y target\pharma-erp-1.0.0.jar build-input\ >nul

echo [3/3] Running jpackage...
if exist dist rmdir /s /q dist
mkdir dist

REM Detect WiX (candle.exe) to decide installer vs portable app-image.
where candle.exe >nul 2>&1
if %errorlevel%==0 (
    echo WiX found - building .exe installer with shortcuts...
    "%JP%" --type exe --name PharmaERP --input build-input ^
      --main-jar pharma-erp-1.0.0.jar ^
      --main-class org.springframework.boot.loader.launch.JarLauncher ^
      --dest dist --app-version 1.0.0 --vendor "PharmaERP" ^
      --java-options "-Xmx512m" ^
      --win-shortcut --win-menu --win-dir-chooser --win-per-user-install ^
      --icon build-app\PharmaERP\PharmaERP.ico || goto :error
    echo.
    echo DONE. Installer is in the dist\ folder: dist\PharmaERP-1.0.0.exe
) else (
    echo WiX not found - building portable app-image instead...
    "%JP%" --type app-image --name PharmaERP --input build-input ^
      --main-jar pharma-erp-1.0.0.jar ^
      --main-class org.springframework.boot.loader.launch.JarLauncher ^
      --dest dist --app-version 1.0.0 --vendor "PharmaERP" ^
      --java-options "-Xmx512m" || goto :error
    echo.
    echo DONE. Portable app is in dist\PharmaERP\ (run PharmaERP.exe).
    echo To get a double-click installer instead, install WiX and re-run this.
)
goto :eof

:error
echo BUILD FAILED.
exit /b 1
