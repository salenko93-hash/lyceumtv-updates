# Analysis and fixes — LyceumTV 2.7.0.9 Premium Broadcast

## Changes made for 2.7.0.9

- redesigned `SignageView` as a modern Premium Broadcast TV dashboard;
- normal schedule is shown as eight large remote-display-friendly cards in a 2×4 grid;
- AIR_RAID uses a full red broadcast background and the shelter schedule;
- ALL_CLEAR uses a full green broadcast screen for 15 seconds;
- alert source and alert start time are shown on the red screen;
- added a restrained 250 ms mode transition;
- supplied lyceum logo and full institution name remain integrated;
- numerator start is fixed by default to 01.09.2026 and may be any weekday;
- alternating weeks are calculated from the Monday containing the configured start date;
- admin dialog is no longer closed or blocked by an active alert;
- admin now has immediate API refresh, manual alarm and manual all-clear controls;
- explicit admin synchronization is allowed during AIR_RAID while SHA/schema/atomic checks remain;
- old Android compatibility improved by removing runtime `String.isBlank()` calls;
- `Europe/Kyiv` -> `Europe/Kiev` timezone fallback remains;
- version bumped to `versionCode 49` / `versionName 2.7.0.9`.

## Deliberately retained safety behavior

The admin is fully reachable during AIR_RAID, but a manual all-clear does not falsify the
confirmed alerts.in.ua state. If UID 81 remains ACTIVE, clearing the manual flag still
leaves the red alert screen active. Minute-of-silence playback is not started over a real
active alert.

## Production data retained

- four real timetable assets;
- 8 classes;
- 11 bell slots;
- 2026/2027 academic calendar;
- original 79.536 s MP3;
- fixed alerts.in.ua UID 81 and encrypted token storage.

The source workbook discrepancies documented in `DATA_IMPORT_REPORT.md` are still preserved
rather than guessed or silently corrected.
