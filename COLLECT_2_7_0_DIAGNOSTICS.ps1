param([string]$Tv = "192.168.5.77:5555")
$ErrorActionPreference = "Continue"
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb connect $Tv | Out-Host

Write-Host "`n== PACKAGE =="
& $adb -s $Tv shell dumpsys package ua.edu.cunl.tv.debug |
    Select-String "versionCode|versionName"

Write-Host "`n== ALERTS UID / CONFIRMED STATE =="
& $adb -s $Tv shell run-as ua.edu.cunl.tv.debug `
    cat shared_prefs/alerts_in_ua_monitor_v1.xml

Write-Host "`n== ADMIN SETTINGS (token is stored separately/encrypted) =="
& $adb -s $Tv shell run-as ua.edu.cunl.tv.debug `
    cat shared_prefs/admin_settings_v1.xml

Write-Host "`n== LOGS =="
& $adb -s $Tv logcat -d -t 2000 |
    Select-String "LyceumAlertsApi"
