@echo off
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\INSTALL_EXISTING_APK.ps1"
echo.
pause
