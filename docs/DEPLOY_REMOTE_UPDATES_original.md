# LyceumTV 2.7.0.1 — Remote deployment walkthrough

## Option 1: Local server / NAS

1. Open the included `sample-server` folder. It contains all **four** current real schedule JSON files, `calendar.json`, `content.json` and `content_manifest.json`, with matching SHA-256 hashes.
2. Modify the actual timetable JSON only when an official schedule update is approved. Preserve all eight class IDs and the two shelter files.
3. Edit `content.json` announcements and `substitutions`. Examples are included, but the sample substitution is disabled (`active: false`).
4. Enter actual school holidays into `calendar.json`. The initial calendar is deliberately empty.
5. Each time JSON changes, increment manifest version and regenerate checksums from PowerShell in the project root:

   ```powershell
   python .\tools\make_manifest.py --folder sample-server --version 2026-10-01-2
   ```

6. Publish the **contents** of `sample-server` to your trusted web server/NAS. For a temporary local test with Python installed on a Windows PC:

   ```powershell
   cd .\sample-server
   python -m http.server 8000 --bind 0.0.0.0
   ```

   Determine that PC's local IPv4 using `ipconfig`. Check that the PC firewall allows TCP/8000 from the TV's private network and the PC remains powered on. A sample URL is `http://YOUR_LAN_IPV4:8000/content_manifest.json`; use a **numeric private LAN IPv4** address, not `localhost`, and ideally a proper HTTPS NAS instead.

7. On TV, hold OK → select `ЛОКАЛЬНИЙ СЕРВЕР / NAS` → enter manifest URL → save → synchronize. The embedded package remains usable while the local server is offline.
8. To host a newer APK, build it with the **same signing certificate**, ensure versionCode is higher than the installed version, then run in the project root:

   ```powershell
   python .\tools\make_manifest.py --folder sample-server --version 2026-10-01-3 --apk PATH_TO_NEW_SIGNED_APK --apk-code 41
   ```

   The script copies it as `LyceumTV.apk` and writes real SHA-256. Confirm the *actual APK* has the supplied versionCode; the TV also checks it. Synchronize, then use admin `ПЕРЕВІРИТИ ОНОВЛЕННЯ APK` and `ЗАВАНТАЖИТИ Й УСТАНОВИТИ APK`.

## Option 2: GitHub Releases + Google Sheets

1. Host a `content_manifest.json` and **four schedule JSONs** (plus optionally `calendar.json`) in a GitHub repository. Run the manifest generator on the folder before committing JSON changes. The project does **not** include GitHub authentication; the raw manifest and schedules must be readable by the TV.
2. Add the repository path `owner/repository` and the corresponding full `https://raw.githubusercontent.com/.../content_manifest.json` in the TV admin menu.
3. Deploy the included `apps-script/Code.gs` against your Google spreadsheet as a Web App that returns JSON. Its output includes `announcements`, `events`, `schedule`, `substitutions` and `settings`. Store announcements in sheet `Announcements` and operational substitutions in sheet `Substitutions`.
4. `Substitutions` header row exactly:

   ```text
   date | week | day | lesson | class | scope | active | entries_json
   ```

   Example fields: `2026-10-05`, `all`, `MONDAY`, `2`, `10-А`, `normal`, `TRUE`, `[{"subject":"Алгебра","room":"12","teacher":"ПІБ"}]`. Use dates from the approved school schedule; `[]` means cancelled lesson. Re-deploy the Apps Script Web App after code edits.
5. Paste that HTTPS Google Apps Script URL into the existing `Google Apps Script Web App URL` admin field. Select `GITHUB + GOOGLE SHEETS`. Save and sync.
6. For app upgrades, create a public GitHub Release in the configured repository. Add two **matching assets** named exactly:
   - `LyceumTV.apk` (higher versionCode, identical installed package ID and signer);
   - `LyceumTV.apk.sha256` (SHA-256 checksum text, optionally followed by the filename).

   Put a separate line in the GitHub Release description:

   ```text
   versionCode=41
   ```

7. TV automatically checks approximately every six hours, and admin can check manually. Downloading/installing requires admin action and Android system confirmation. No silent install or install during an alarm.

## Scheduling and recovery

- File switching happens only after all four SHA-256 checks and schema checks pass and the alarm state is safe **again at commit time**.
- If no network, last downloaded files are read; if cache is absent/corrupt, packaged assets are used.
- Admin `ПОВЕРНУТИ ПОПЕРЕДНІ РОЗКЛАДИ` restores the previous downloaded bundle, or embedded schedules if there is no previous download. A rolled-back remote version is blocked until its manifest version changes.
- Avoid changing approved shelter routes during an actual active alert; updates are delayed.
- Holiday calendar is manually supplied; app does not guess national holidays or lessons during martial law.
- NTP check uses UDP/123; if blocked, the clock warning remains advisory. The TV's automatic date/time setting should remain enabled.
- After reboot, the existing watchdog and boot receiver make a best-effort start. Xiaomi may still require enabling Autostart and disabling aggressive power restrictions.

## Signing warning

The TV's existing `ua.edu.cunl.tv.debug` build is signed with the debug key from the PC that built it. An APK signed with a different key or built as package `ua.edu.cunl.tv` cannot update it in place. The updater rejects mismatched package name, signer, SHA-256 and version.
