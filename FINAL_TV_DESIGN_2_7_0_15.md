# LyceumTV 2.7.0.15 — FINAL TV DESIGN

## Scope
This release consolidates every approved visual correction into one project.

## Pages
1. **Normal lesson** — blue layout, uniform subject typography, smaller teacher typography, lesson countdown, compact Kropyvnytskyi weather.
2. **Break** — same layout, next lesson in side cards, `ПЕРЕРВА`, `ДО ПОЧАТКУ N УРОКУ`, live countdown, weather, optional announcement strip.
3. **Shelter / air raid** — red companion layout, shelter schedule, emergency instruction, current lesson/break countdown, weather.
4. **Minute of silence** — dark navy memorial page with improved candle; no playback caption and no technical footer.

## Admin migration
`applicationId` and `AdminDialog.PREFS = admin_settings_v1` are unchanged. Updating with `pm install -r` preserves the existing preferences and the secure alerts.in.ua token.

The migration writes defaults only for empty/missing fields:
- `numerator_start_date = 2026-09-01`
- GitHub/NAS Manifest URL
- Google Apps Script URL
- screen profile and existing toggle/volume defaults

Existing non-empty values are not overwritten.

## Default remote sources
Manifest:
`https://raw.githubusercontent.com/salenko93-hash/lyceumtv-updates/main/sample-server/content_manifest.json`

Google Apps Script:
`https://script.google.com/macros/s/AKfycbwoo2SlQh3ojGRycG9EmUMhAqOgIhMlN9lCw6BzVDGlihVWAYonx1YTmUE8ap7EDKgSEA/exec`

## Reference images
See `docs/mockups/`:
- `normal_schedule_final_1920x1080.png`
- `break_schedule_final_1920x1080.png`
- `shelter_schedule_final_1920x1080.png`
- `minute_silence_final_reference_1920x1080.png`

## Data kept
- four production schedule JSON files;
- numerator reference `01.09.2026`;
- alerts.in.ua UID 81;
- `TRIVOGA.mp3`;
- minute-of-silence audio;
- GitHub/NAS + Apps Script sync;
- SHA-256/schema validation and atomic remote bundle swap.
