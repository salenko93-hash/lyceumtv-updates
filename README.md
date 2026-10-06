# LyceumTV 2.7.0.4 — запис хвилини мовчання + оголошення під час перерв

Android TV signage for the Central Ukrainian Scientific Lyceum.

## Deployment constants

- **Only automatic alarm source:** alerts.in.ua, UID **81** (Kropyvnytskyi district / raion).
- Default TV ADB address: `192.168.5.77:5555`.
- App version: `versionCode=44`, `versionName=2.7.0.4`.
- The API token remains protected by Android Keystore-backed AES/GCM.
- Previous emergency controls remain: hold ↑ manual alarm, hold ↓ manual all-clear.
- Active confirmed alarm always overrides ordinary display and uses the shelter schedule.
- Network errors **never** imply all-clear. Official civil protection alerts remain authoritative.

## Included features

1. **Remote schedules:** validated, SHA-256 checked, four-file atomic bundle from either GitHub HTTPS manifest or a local HTTP(S) NAS manifest. Every set includes numerator, denominator and both shelter schedules. Refresh: ~15 minutes plus admin manual sync.
2. **Offline protection:** packaged four-file schedules + last known downloaded bundle + previous downloaded bundle for rollback. A bad version can be rolled back and is blocked from automatic reinstall until the publisher increments the version.
3. **Operational substitutions:** higher-priority `substitutions` entries from the selected source's `content.json` or Google Sheets JSON feed. `scope=normal|shelter|both`, date/day/week/class/lesson targeting, one or more group entries. A matched `entries:[]` cancels that class lesson.
4. **Holiday calendar:** `calendar.json` in the manifest; public holidays and school breaks are maintained by the administrator, not guessed.
5. **Alternating weeks:** reference numerator Monday configured in admin; every next Monday alternates.
6. **After school:** after the day's last scheduled lesson (from the currently selected timetable, including shorter Saturdays) shows time, weather and cached rotating announcements. Air-raid mode remains higher priority.
7. **Display profiles:** AUTO, FHD (1920x1080), HD (1280x720), OTHER (overscan-safe). All use aspect-preserving proportional scaling.
8. **NTP check:** `time.google.com` via UDP/123 every 6h; warns if drift exceeds 30s. Never modifies system clock or changes alarm sources.
9. **Startup/recovery:** existing BOOT_COMPLETED/MY_PACKAGE_REPLACED and foreground watchdog preserved, USER_UNLOCKED added. HyperOS/Android background launch rules may require enabling autostart and manual launch once after boot.
10. **APK update:** automatic latest-release check about every six hours; administrator initiates downloading and approves system installation. Exact APK SHA-256, Android package name, versionCode and installed signing certificate are checked. No installation allowed during AIR_RAID or ALL_CLEAR. Android TV normally prohibits silent installations from an ordinary app.

## Quick install

Extract FULL ZIP, open PowerShell in the `LyceumTV-2.7.0.4-UID81-SILENCE-MP3` folder, first compile **without touching the live TV**:

```powershell
.\BUILD_AND_INSTALL.ps1 -BuildOnly
```

After a successful build, install outside an active alert:

```powershell
.\BUILD_AND_INSTALL.ps1
```

The deployment script already uses `192.168.5.77:5555`.
Do **not** use `-FreshInstall` when upgrading an existing installation: it would delete the protected alerts.in.ua token.

Verify:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb -s "192.168.5.77:5555" shell dumpsys package ua.edu.cunl.tv.debug |
  Select-String "versionName|versionCode"
```

Expected `versionCode=44`, `versionName=2.7.0.4-debug`.

## Admin and selected update source

Hold OK (or press MENU) on the TV remote.

- Select `GITHUB + GOOGLE SHEETS` or `ЛОКАЛЬНИЙ СЕРВЕР / NAS`.
- For GitHub/Sheets, configure `GitHub repository = owner/repository`, HTTPS URL to GitHub raw `content_manifest.json` for **schedules/calendar**, and the deployed Google Sheets Apps Script Web App URL for **announcements/substitutions**.
- For LOCAL, configure the HTTP(S) URL of `content_manifest.json` on the same private LAN or a trusted HTTPS NAS. This manifest drives **schedules/calendar/content and APK**.
- Configure reference Monday of numerator (e.g. `2026-08-31`) and screen profile.
- Save, then select `СИНХРОНІЗУВАТИ ЗАРАЗ`.

The project does not contain live GitHub/Google/NAS account URLs: you provide them in admin.
The **alerts.in.ua UID 81 remains fixed** and cannot be altered by remote content or the admin menu.

For full hosting examples, see `DEPLOY_REMOTE_UPDATES.md`.

## Wire formats

`sample-server/content_manifest.json`:

```json
{
  "version": "2026-10-01-initial",
  "schedules": {
    "schedule_numerator.json": {"url": "schedule_numerator.json", "sha256": "<64 hex>"},
    "schedule_denominator.json": {"url": "schedule_denominator.json", "sha256": "<64 hex>"},
    "shelter_numerator.json": {"url": "shelter_numerator.json", "sha256": "<64 hex>"},
    "shelter_denominator.json": {"url": "shelter_denominator.json", "sha256": "<64 hex>"}
  },
  "calendar": {"url": "calendar.json", "sha256": "<64 hex>"},
  "content": {"url": "content.json", "sha256": "<64 hex>"},
  "apk": {"url": "LyceumTV.apk", "sha256": "<64 hex>", "versionCode": 44}
}
```

The included actual example manifest has real SHA-256 checksums and no APK placeholder.
The `"apk"` field is only added after publishing a newer, validly signed APK.

`calendar.json`: `daysOff:[{"date":"2026-10-14","label":"Канікули"}]`, `ranges:[{"from":"2026-12-28","to":"2027-01-10","label":"Зимові канікули"}]`. These are *illustrative*, not an assertion about the real school calendar; the shipped calendar is empty.

`content.json` / Google Sheets output: `announcements`, `events`, `schedule` (legacy), `substitutions`. See `sample-server/content.json` for an initially disabled substitution. Substitute entries override a specific class/lesson without modifying the underlying timetable bundle.

**Safety:** avoid publishing nonpublic shelter/classroom details in a public GitHub repository. Prefer an access-controlled trusted HTTPS server/NAS where appropriate. If hosting via LAN HTTP, only numeric private LAN IPv4 hosts are accepted; plain HTTP offers no confidentiality against LAN attackers. Use HTTPS for high-assurance deployments.

## Build-fix revision

The earlier 2.7.0.1 revision added SignageView.setScreenProfile(String)
and setClockStatus(String) to resolve javac errors. Those fixes remain present
in version 2.7.0.3. The alerts.in.ua UID is still 81..

## 2.7.0.2 history: Minute-of-silence metronome (replaced in 2.7.0.4)

- Quiet locally bundled metronome sample (not MP3, not streamed): **60 BPM**.
- Automatically plays **only in the SILENCE display mode** (daily 09:00–09:01
  Europe/Kyiv and the admin 60-second silence test).
- Separate Settings -> Metronome switch (default ON) and volume (default 25%).
  Volume 0 or switch OFF means the minute remains silent.
- Alarm UID remains fixed to **81**. Confirmed API ACTIVE/PARTIAL or manual
  ALARM stops a playing click immediately; the sound is also stopped when
  the activity moves to background or when the 60-second period ends.
- Audio uses the normal Android media stream and obeys the TV's media volume.
  If another application produces alert MP3, that is independent of LyceumTV.
- Same four normal/shelter schedules, Google Sheets announcements, GitHub +
  local NAS updates, and TV ADB `192.168.5.77:5555`.


## 2.7.0.3: оголошення під час перерв

- Ті самі оголошення з Google Sheets. На *реальних перервах* 30 секунд
  показується розклад, потім 15 секунд повноекранне активне оголошення.
- Останні 60 секунд перед дзвінком завжди відведено під розклад.
- Кілька активних оголошень автоматично змінюють одне одного; рядки
  `active=FALSE` приховано. Події з вкладки Events на перервах не показуються.
- В адмінці є окремий перемикач і кнопка тесту на 20 секунд.
- Під час тривоги, відбою, хвилини мовчання, уроків, канікул і після уроків
  зберігається відповідна попередня логіка. Тест скасовується при тривозі.
- Версія `2.7.0.3`, versionCode `43`. Не використовуйте `-FreshInstall`.



## 2.7.0.4 — Надана користувачем фонограма замість метронома

- Оригінальний MP3 `03 хвилина мовчання.mp3` вбудовано без перекодування
  як `app/src/main/res/raw/minute_silence.mp3`; тривалість ffprobe: 79.536 с.
- О 09:00 за київським часом розпочинається відтворення запису один раз.
  Екран залишається в режимі хвилини мовчання **до завершення фонограми**,
  приблизно 09:01:19.536 (на екрані округлення до 09:01:20).
  Система отримує фактичну тривалість із MediaPlayer під час запуску.
- Тест з адмінки також триває повну довжину фонограми.
- Наявні налаштування увімкнення й гучності від старого метронома
  автоматично зберігаються, але тепер управляють саме MP3.
- При підтвердженій/ручній тривозі або переході в інший Android-застосунок
  аудіо негайно зупиняється. Відтворення не зациклюється.
- UID 81, всі 4 комплекти розкладів, Google Sheets, оголошення на перервах
  та безпекові обмеження віддалених оновлень залишено без змін.
- `versionCode=44`, `versionName=2.7.0.4`. Збирати й оновлювати на Windows
  без видалення вже встановленої debug-версії.
- Для FULL/HOTFIX: `INSTALL_SILENCE_MP3_2_7_0_4.md`.
