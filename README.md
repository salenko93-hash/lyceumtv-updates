# LyceumTV 2.7.0.14 — Approved TV Pages

Android TV signage for the **Центральноукраїнський науковий ліцей Кіровоградської обласної ради**.

## Version
- versionCode: **54**
- versionName: **2.7.0.14** (`2.7.0.14-debug` for the debug APK)
- package for debug install: `ua.edu.cunl.tv.debug`
- alerts.in.ua UID: **81**
- default TV ADB: `192.168.5.77:5555`

## Visual layout
Version 2.7.0.14 implements the two approved 16:9 TV pages directly in Android Canvas:

### Normal schedule
- blue edge-to-center gradient matching the approved mockup;
- white cropped lyceum logo on the central axis;
- large `РОЗКЛАД УРОКІВ` title;
- orange `ЧИСЕЛЬНИК / ЗНАМЕННИК` pill;
- weekday, date and large clock;
- four 10th-grade cards on the left and four 11th-grade cards on the right;
- large lesson/break countdown card;
- bottom Kropyvnytskyi weather panel with current conditions and the next four days.

### Shelter / air-raid page
- identical geometry so the screen does not jump when mode changes;
- red emergency gradient;
- `РОЗКЛАД В УКРИТТІ`;
- the same class-card grid, but using the shelter schedule JSON;
- large current time;
- `ПОВІТРЯНА ТРИВОГА` + `НЕГАЙНО ПРОЙДІТЬ В УКРИТТЯ`;
- lesson/break indicator; weather is intentionally hidden during an alert.

Typography uses Android's system Roboto / Roboto Medium family only. No external font files are required.
The week containing **01.09.2026** is numerator. Every following Monday alternates numerator/denominator.

Approved reference images are included in:
- `docs/mockups/normal_schedule_approved.png`
- `docs/mockups/shelter_schedule_approved.png`

## Structured lesson cards
Each class card shows the active/next lesson as separate fields:
- class and lesson number;
- subject name;
- room as a dedicated `КАБ.` badge;
- teacher in a separate area;
- explicit `гр.1`, `гр.2`, `група`, or `підгрупа` source markers are rendered as separate subgroup blocks.

No class/subject/room/teacher data is invented. If the source workbook/JSON leaves a field empty, the UI shows an empty/dash value rather than guessing.

## Weather and logo
The original supplied `lyceum_logo.png` remains unchanged for the app icon.
The TV/admin UI uses a separate cropped **white** `lyceum_logo_white.png`, preserving
transparency and adding a subtle dark halo so the mark stays visible on blue/red backgrounds.

The main screen fetches Kropyvnytskyi weather from Open-Meteo over HTTPS
(no API key): current temperature, apparent temperature, WMO weather code, plus daily
min/max temperatures and weather codes for five days. The TV page renders today and
the next four-day forecast. Normal refresh interval is 10 minutes; connection failures
retry after 2 minutes. Weather is hidden on the active shelter/air-raid page.

## Break announcements
When `ОГОЛОШЕННЯ НА ПЕРЕРВАХ` is enabled in admin:
- first 30 seconds of each 45-second cycle show the schedule;
- next 15 seconds show one active announcement;
- the final 60 seconds before the next lesson always show the schedule;
- active announcements rotate in source order;
- `active=false` hides an announcement;
- when there are no active announcements, the schedule remains on screen.

Announcements are read from the last successfully synchronized `content.json`
(remote cache first, packaged fallback second). Common fields `message`, `text`,
`body`, `content`, and optional `title` are supported.

## Build
```powershell
python .\VERIFY_SOURCE_UID81.py
.\CHECK_BUILD_ENV.ps1
.\BUILD_AND_INSTALL.ps1 -BuildOnly
```

Continue only after `BUILD SUCCESSFUL`.

Install over the existing debug application:
```powershell
.\BUILD_AND_INSTALL.ps1
```

Do **not** use `-FreshInstall` if the existing API token and settings must be preserved.

## API token from PC
After installing the debug APK:
```powershell
.\SET_ALERTS_TOKEN.ps1
```

The token is handed through the app-private sandbox, imported into the
Android Keystore-backed secure store, and the plaintext hand-off file is deleted.

## Admin
Hold OK for about one second, or use MENU/SETTINGS. Admin remains accessible
during an active alert. The shelter screen remains red while the confirmed or
manual alarm is active after the admin dialog is closed.

## 2.7.0.14 approved-page update
- Rebuilt `SignageView` around the approved blue/red page pair.
- Larger TV-readable schedule typography and fixed table columns.
- Normal and shelter pages share the same geometry.
- Added current weather + next four-day forecast to the normal page.
- Removed the tiny diagnostic footer from NORMAL and AIR_RAID screens.
- Preserved all real schedule data, UID 81 logic, alert audio and minute-of-silence audio.

## 2.7.0.13 data/audio update
The packaged normal and shelter schedules are regenerated from the latest supplied XLSX files:
- `Розклад 10-11 Чисельник(6).xlsx`
- `Розклад 10-11 Знаменник(5).xlsx`
- `РОЗКЛАД  10–11 КЛАСІВ — ЧИСЕЛЬНИК УКРИТТЯ(4).xlsx`
- `РОЗКЛАД 10–11 КЛАСІВ — ЗНАМЕННИК УКРИТТЯ(3).xlsx`

The source workbooks contain lessons 1–8, with times 08:00–15:15. Excel continuation
rows for groups are preserved; when a merged subject cell is blank on a continuation row,
the subject is inherited from the preceding row of that same lesson.

`TRIVOGA.mp3` is packaged as `res/raw/trivoga.mp3` and is played once when the app
enters an active air-raid state. It is not looped. The admin screen can enable/disable
the sound and set its volume. The supplied `03 хвилина мовчання(3).mp3` is packaged
as `res/raw/minute_silence.mp3`.
