@echo off
REM ============================================================
REM  Creates a Desktop shortcut for the portable PharmaERP app.
REM  Run this once after copying the PharmaERP app folder.
REM  It points the shortcut at PharmaERP.exe in the same tree.
REM ============================================================

setlocal
set "APPDIR=%~dp0PharmaERP"
if not exist "%APPDIR%\PharmaERP.exe" set "APPDIR=%~dp0..\build-app\PharmaERP"

if not exist "%APPDIR%\PharmaERP.exe" (
    echo Could not find PharmaERP.exe.
    echo Place this script next to the PharmaERP app folder and try again.
    pause
    exit /b 1
)

set "TARGET=%APPDIR%\PharmaERP.exe"
set "ICON=%APPDIR%\PharmaERP.ico"

REM Resolve the real Desktop path (handles OneDrive-redirected desktops).
powershell -NoProfile -Command ^
  "$desk=[Environment]::GetFolderPath('Desktop');" ^
  "$link=Join-Path $desk 'PharmaERP.lnk';" ^
  "$s=(New-Object -ComObject WScript.Shell).CreateShortcut($link);" ^
  "$s.TargetPath='%TARGET%';" ^
  "$s.WorkingDirectory='%APPDIR%';" ^
  "$s.IconLocation='%ICON%';" ^
  "$s.Description='PharmaERP';" ^
  "$s.Save();" ^
  "Write-Host ('Desktop shortcut created: ' + $link)"

pause
