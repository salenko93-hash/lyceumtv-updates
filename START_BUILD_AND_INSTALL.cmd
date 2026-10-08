@echo off
setlocal
cd /d "%~dp0"
echo Starting LyceumMobile build and install...
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0BUILD_AND_INSTALL.ps1"
set "RC=%ERRORLEVEL%"
echo.
if not "%RC%"=="0" (
  echo Build/install failed with exit code %RC%.
) else (
  echo Build/install completed successfully.
)
pause
exit /b %RC%
