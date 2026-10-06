@echo off
setlocal
cd /d "%~dp0"
echo Starting LyceumMobile build only...
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0BUILD_AND_INSTALL.ps1" -BuildOnly
set "RC=%ERRORLEVEL%"
echo.
if not "%RC%"=="0" (
  echo Build failed with exit code %RC%.
) else (
  echo Build completed successfully.
)
pause
exit /b %RC%
