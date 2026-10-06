# LyceumTV 2.7.0.2 — UID 81 + quiet metronome

This is a **source project**, not a precompiled APK. Build the debug APK on
the Windows computer where your JDK 17 and Android SDK are installed.

## Settings and behavior

- Fixed alerts.in.ua UID: `81` (Kropyvnytskyi district).
- Version: `versionCode 42`, `versionName 2.7.0.2-debug`.
- Default TV address for scripts: `192.168.5.77:5555`.
- Metronome: enabled by default; 60 BPM, initially 25% volume.
- Metronome sounds only while the minute-of-silence screen is displayed,
  at 09:00–09:01 Kyiv time or during a 60-second admin silence test.
- On confirmed alarm, manual alarm, exit from SILENCE, or backgrounding,
  the metronome stops.
- API errors never imply all-clear. Metered audio cannot override alert logic.
- GitHub/Google Sheets, NAS, all schedule JSONs, and encrypted alert token stay
  unchanged. **Do not use `-FreshInstall`** when upgrading.

## FULL — install clean source folder but preserve TV app data

1. Extract `LyceumTV_2_7_0_2_UID81_METRONOME_FULL.zip`.
2. Open PowerShell inside `LyceumTV-2.7.0.2-UID81-METRONOME`.
3. Check compilation first, with TV unaffected:

```powershell
.\BUILD_AND_INSTALL.ps1 -BuildOnly
```

4. Only after seeing `BUILD SUCCESSFUL`, and outside an active emergency:

```powershell
.\BUILD_AND_INSTALL.ps1
```

## HOTFIX — patch an existing FIXED 2.7.0.1 project

Extract `LyceumTV_2_7_0_2_UID81_METRONOME_HOTFIX.zip` over the project root
(the folder containing `BUILD_AND_INSTALL.ps1`), accepting overwrite.
The HOTFIX includes the modified/new source, build version, resource and tests.

Run the same two build commands above.

## Test metronome on TV

1. Open LyceumTV, hold **OK** to open the admin screen.
2. Scroll down to **МЕТРОНОМ ХВИЛИНИ МОВЧАННЯ**.
3. Check **Увімкнути тихий метроном** and set volume to 25% (or lower).
4. Save, reopen admin, choose **ТЕСТ ХВИЛИНИ МОВЧАННЯ З МЕТРОНОМОМ • 60 СЕК**.
5. Confirm exactly one quiet click per second while the SILENCE screen
   is shown, then silence. If needed, turn volume to 0 to mute it.
6. Confirm normal 09:00–09:01 operation separately on the next school day.

Testing the alarm interrupt should only use the app's **admin test or manual
controls** and only outside actual emergencies. A confirmed alarm always has
higher priority.

## Android build / installed version check

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$tv  = "192.168.5.77:5555"
& $adb connect $tv
& $adb -s $tv shell dumpsys package ua.edu.cunl.tv.debug |
    Select-String "versionCode|versionName"
```

Expected: `versionCode=42`, `versionName=2.7.0.2-debug`.

## Diagnostics

```powershell
& $adb -s $tv logcat -d -t 2000 |
    Select-String "LyceumMetronome|LyceumAlarmMain"
```

**Safety**: LyceumTV is supplementary signage; always rely on official
emergency warning channels and responsible personnel for decisions.
