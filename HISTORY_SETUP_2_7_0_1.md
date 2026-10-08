# LyceumTV 2.7.0.1 — Setup

Unzip the FULL source and open PowerShell inside `LyceumTV-2.7.0.1-UID81`.

First build safely without installation:

```powershell
.\BUILD_AND_INSTALL.ps1 -BuildOnly
```

After successful compilation, install while there is no active alert:

```powershell
.\BUILD_AND_INSTALL.ps1
```

Default TV: `192.168.5.77:5555`. To use a different reachable address:

```powershell
.\BUILD_AND_INSTALL.ps1 -Tv "OTHER_IP:5555"
```

Do not use `-FreshInstall` if you want to preserve your alerts.in.ua token, alarm preferences and cached schedules.

Version after install:

```text
versionCode=41
versionName=2.7.0.1-debug
```

Hold OK on the TV remote, configure update source (GitHub+Google Sheets or local NAS), source URLs, school week reference date, NTP and display profile.

Before using production update channels, follow `DEPLOY_REMOTE_UPDATES.md` to publish **real** JSON files, manifest checksums, and (later) a compatible signed APK.

Keep official alert channels available; this signage screen is an auxiliary display.

## Territory switch: UID 761 -> UID 81

This build has **one fixed alerts.in.ua UID: 81** (Kropyvnytskyi district).
It no longer monitors only the city/community. An `A` or `P` response from
the district endpoint activates the shelter timetable.

When updating from UID 761, the previous alarm HTTP validator, confirmed
state and territory cache are discarded automatically on first launch.
The encrypted API token and existing remote-schedule configuration are kept.

This is an auxiliary information screen. Follow official alerts and
responsible staff instructions during an emergency.
