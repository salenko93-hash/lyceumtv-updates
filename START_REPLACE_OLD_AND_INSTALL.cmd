@echo off
cd /d "%~dp0"
echo WARNING: this removes old LyceumMobile app data/settings if signatures conflict.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\INSTALL_EXISTING_APK.ps1" -ReplaceExisting
echo.
pause
