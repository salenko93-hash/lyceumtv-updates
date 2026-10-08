# LyceumTV 2.7.0.1 — UID 81 (Kropyvnytskyi district)

This is LyceumTV 2.7.0 with only the alarm territory changed:
**alerts.in.ua UID 81**. The TV's default ADB endpoint remains
**192.168.5.77:5555**.

- versionCode: **41** (upgrade over 2.7.0 / code 40)
- debug versionName: **2.7.0.1-debug**
- only automatic alarm source: alerts.in.ua
- normal and shelter timetables: all four preserved
- GitHub + Google Sheets / local NAS source selection: unchanged
- A/P for district UID 81: alarm + shelter schedule
- N after active: all-clear 15 seconds, then normal schedule
- timeouts: preserve last confirmed state; no false all-clear

## 1. Compile without affecting the TV

```powershell
.\BUILD_AND_INSTALL.ps1 -BuildOnly
```

## 2. Install outside an active emergency

```powershell
.\BUILD_AND_INSTALL.ps1
```

Do NOT use `-FreshInstall`, which would delete the encrypted API token.
The install script already defaults to `192.168.5.77:5555`.

On first launch the previous UID 761 cached state is invalidated; the
first successful API fetch establishes the new confirmed district state.

## 3. Verify version and territory

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$tv = "192.168.5.77:5555"
& $adb connect $tv
& $adb -s $tv shell dumpsys package ua.edu.cunl.tv.debug |
    Select-String "versionCode|versionName"
& $adb -s $tv shell run-as ua.edu.cunl.tv.debug `
    cat shared_prefs/alerts_in_ua_monitor_v1.xml |
    Select-String "last_uid|last_http|last_raw|confirmed_state"
```

Expected: versionCode=41, versionName=2.7.0.1-debug and `last_uid` = `81`
after the first API request.

## 4. Direct test from Windows

```powershell
.\TEST_ALERTS_IN_UA_API.ps1
```

The script calls only
`https://api.alerts.in.ua/v1/iot/active_air_raid_alerts/81.json`.
It prompts securely for your API token.

## 5. Diagnostics

```powershell
.\COLLECT_2_7_0_DIAGNOSTICS.ps1
```

See `LYCEUMTV_2_7_0_1_UID81_SETUP.md` for original 2.7 deployment guidance.

**Important:** District UID 81 is broader than Kropyvnytskyi community
UID 761. As configured, a partial `P` anywhere within this district
also causes an alarm display.
