param([string]$Tv = "192.168.5.77:5555")
$ErrorActionPreference = "Continue"
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb connect $Tv | Out-Host
Write-Host "`n== PACKAGE =="
& $adb -s $Tv shell dumpsys package ua.edu.cunl.tv.debug |
    Select-String "versionCode|versionName"
Write-Host "`n== LOCKED ALERTS UID =="
Write-Host "Expected only API UID: 81"
Write-Host "`n== ALARM API STORE =="
& $adb -s $Tv shell run-as ua.edu.cunl.tv.debug `
    cat shared_prefs/alerts_in_ua_monitor_v1.xml
Write-Host "`n== REMOTE CONTENT STATUS =="
& $adb -s $Tv shell run-as ua.edu.cunl.tv.debug `
    cat shared_prefs/remote_sync_status_v1.xml
Write-Host "`n== NTP STATUS =="
& $adb -s $Tv shell run-as ua.edu.cunl.tv.debug `
    cat shared_prefs/ntp_status_v1.xml
Write-Host "`n== LOGS (no encrypted token output) =="
& $adb -s $Tv logcat -d |
    Select-String "LyceumAlertsApi|LyceumAlarmMain|LyceumAlarmState|LyceumRemoteContent|LyceumNTP|LyceumApk"
