param(
    [string]$Tv = "192.168.5.77:5555"
)

$ErrorActionPreference = "Continue"

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

Write-Host "== CONNECT =="
& $adb connect $Tv

Write-Host ""
Write-Host "== PACKAGE VERSION =="
& $adb -s $Tv shell dumpsys package ua.edu.cunl.tv.debug |
    Select-String "versionCode|versionName"

Write-Host ""
Write-Host "== ALERTS.IN.UA FIXED UID 81 STORE =="
& $adb -s $Tv shell run-as ua.edu.cunl.tv.debug `
    cat shared_prefs/alerts_in_ua_monitor_v1.xml

Write-Host ""
Write-Host "== API-ONLY ALARM LOGS =="
& $adb -s $Tv logcat -d |
    Select-String "LyceumAlertsApi|LyceumAlarmMain|LyceumAlarmState"

Write-Host ""
Write-Host "== POLL CADENCE =="
& $adb -s $Tv logcat -d |
    Select-String "LyceumAlertsApi: poll start|LyceumAlertsApi: scheduler started|LyceumAlertsApi: start ignored|LyceumAlertsApi: pollNow throttled"
