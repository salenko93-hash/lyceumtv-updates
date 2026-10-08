$ErrorActionPreference = "Stop"

Write-Host "Installing Microsoft OpenJDK 17..."
winget install --id Microsoft.OpenJDK.17 -e --source winget

Write-Host ""
Write-Host "Installation command finished."
Write-Host "Close this PowerShell window, open a new one, then run:"
Write-Host ".\BUILD_AND_INSTALL.ps1"
