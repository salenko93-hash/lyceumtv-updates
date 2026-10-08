param([string]$Tv = "192.168.5.77:5555")
$ErrorActionPreference = "Continue"
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb connect $Tv | Out-Host

Write-Host "`n== PACKAGE VERSION =="
& $adb -s $Tv shell dumpsys package ua.edu.cunl.tv.debug |
    Select-String "versionCode|versionName"

Write-Host "`n== ALERTS.IN.UA FIXED UID 81 STORE =="
& $adb -s $Tv shell run-as ua.edu.cunl.tv.debug `
    cat shared_prefs/alerts_in_ua_monitor_v1.xml

Write-Host "`n== ALERT API LOGS =="
& $adb -s $Tv logcat -d -t 2000 |
    Select-String "LyceumAlertsApi"
