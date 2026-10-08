# LyceumTV 2.7.0.16 — FINAL TV DESIGN

Android TV signage for the **Центральноукраїнський науковий ліцей Кіровоградської обласної ради**.

## Version
- versionCode: **56**
- versionName: **2.7.0.16** (`2.7.0.16-debug` for the debug APK)
- debug package: `ua.edu.cunl.tv.debug`
- production namespace/applicationId: `ua.edu.cunl.tv`
- alerts.in.ua region UID: **81**
- default TV ADB target in the install script: `192.168.5.77:5555`

## Final TV pages

### 1. Normal lesson
- blue navy → royal blue → cyan center glow;
- white lyceum logo;
- `РОЗКЛАД УРОКІВ`;
- orange `ЧИСЕЛЬНИК / ЗНАМЕННИК` badge;
- weekday, date and large clock;
- 4 cards for grades 10 on the left and 4 cards for grades 11 on the right;
- one fixed subject text size across every card;
- teacher names intentionally smaller than subjects;
- dedicated room column and separate `ГР.1 / ГР.2` badges;
- current lesson + live `ДО КІНЦЯ УРОКУ` countdown;
- compact current weather for Kropyvnytskyi.

### 2. Break
The geometry does not jump when the lesson ends.
- side cards automatically show the **next lesson**;
- lesson label changes to the next lesson number;
- center card changes to `ПЕРЕРВА`;
- `ДО ПОЧАТКУ N УРОКУ`;
- live break countdown;
- Kropyvnytskyi weather remains visible;
- optional announcement strip remains supported from `content.json`.

### 3. Shelter / air raid
The red page is the direct companion of the blue page and uses the same geometry.
- red/crimson emergency gradient;
- `РОЗКЛАД В УКРИТТІ`;
- shelter schedule JSON instead of the ordinary schedule;
- `ПОВІТРЯНА ТРИВОГА`;
- `НЕГАЙНО ПРОЙДІТЬ В УКРИТТЯ`;
- current lesson number, or break state;
- live lesson/break countdown;
- current Kropyvnytskyi weather;
- uniform subject typography and smaller teacher typography.

### 4. Minute of silence
- dark navy memorial screen;
- white lyceum logo;
- `ХВИЛИНА МОВЧАННЯ`;
- redesigned candle with warm glow, ivory wax and natural flame;
- no `Фонограма відтворюється один раз` caption;
- no technical/status line in the lower-left corner.

The normal/break/shelter TV screens also do **not** render the old diagnostic footer. Diagnostics remain available from the admin screen.

## Numerator / denominator
The configured base date is:

`01.09.2026 = ЧИСЕЛЬНИК`

The following weeks alternate automatically.

## Admin settings are preserved on upgrade
The project deliberately keeps:
- the same package identity;
- `AdminDialog.PREFS = "admin_settings_v1"`;
- all existing preference keys;
- the secure alerts.in.ua token store.

`BUILD_AND_INSTALL.ps1` installs with `pm install -r -t`, so an in-place update keeps Android application data.

**Do not use `-FreshInstall`** when you need the existing admin settings and secure token.

Defaults are written only when the corresponding field is missing or empty. Existing non-empty admin values are never overwritten.

Embedded defaults:
- Manifest URL:
  `https://raw.githubusercontent.com/salenko93-hash/lyceumtv-updates/main/sample-server/content_manifest.json`
- Google Apps Script Web App URL:
  `https://script.google.com/macros/s/AKfycbwoo2SlQh3ojGRycG9EmUMhAqOgIhMlN9lCw6BzVDGlihVWAYonx1YTmUE8ap7EDKgSEA/exec`
- numerator start date: `2026-09-01`

## Real schedule data
The four production schedule bundles are retained:
- `schedule_numerator.json`
- `schedule_denominator.json`
- `shelter_numerator.json`
- `shelter_denominator.json`

They were generated from the supplied XLSX files and are included both in `app/src/main/assets/` and in `sample-server/`.

The remote copy is protected by SHA-256 in `sample-server/content_manifest.json`. Remote updates are validated before the current bundle is replaced.

## Audio
- `TRIVOGA.mp3` → `app/src/main/res/raw/trivoga.mp3`
- supplied minute-of-silence MP3 → `app/src/main/res/raw/minute_silence.mp3`

Air-raid audio plays once when entering the active alarm state. The admin screen keeps enable/disable and volume controls for alarm and minute-of-silence audio.

## Weather
Current weather for **Kropyvnytskyi** is fetched over HTTPS through Open-Meteo without an API key. The compact weather panel is shown on:
- normal lesson page;
- break page;
- shelter / air-raid page.

Normal refresh interval is 10 minutes, with retry after a failed request.

## Final 1920×1080 references
Exact 1920×1080 reference images are included in `docs/mockups/`:
- `normal_schedule_final_1920x1080.png`
- `break_schedule_final_1920x1080.png`
- `shelter_schedule_final_1920x1080.png`
- `minute_silence_final_reference_1920x1080.png`

These PNG files are design references. The actual TV UI is rendered natively by `SignageView`.

## Build and verify on Windows
First run:

```powershell
python .\VERIFY_FINAL_2_7_0_16.py
python .\VERIFY_SOURCE_UID81.py
.\CHECK_BUILD_ENV.ps1
```

Build only:

```powershell
.\BUILD_AND_INSTALL.ps1 -BuildOnly
```

The script downloads its own local Gradle 8.2 distribution into `.gradle-local` if required.

APK output:

```text
app\build\outputs\apk\debug\app-debug.apk
```

Install/update on the existing TV app:

```powershell
.\BUILD_AND_INSTALL.ps1
```

This uses an in-place install so admin settings are retained.

## GitHub / Google Apps Script sync
Default manifest:

```text
https://raw.githubusercontent.com/salenko93-hash/lyceumtv-updates/main/sample-server/content_manifest.json
```

Default Apps Script Web App:

```text
https://script.google.com/macros/s/AKfycbwoo2SlQh3ojGRycG9EmUMhAqOgIhMlN9lCw6BzVDGlihVWAYonx1YTmUE8ap7EDKgSEA/exec
```

Both URLs are already populated in the admin UI when the corresponding saved field is empty.

## Source files retained
The supplied XLSX and MP3 source files are preserved under `source-data/`.

For the final design summary, see `FINAL_TV_DESIGN_2_7_0_15.md`.


## Android TV launcher branding (2.7.0.16)

The project now includes a dedicated LyceumTV launcher icon and a 320×180
Android TV banner. `AndroidManifest.xml` references both assets, while the
package/application ID and admin SharedPreferences names remain unchanged.
This allows an in-place update without intentionally resetting the admin
configuration.
