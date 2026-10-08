# LyceumTV 2.7.0.15 — final design changelog

## TV layout
- Rebuilt the normal lesson page to the approved 1920×1080 design.
- Added a dedicated break page with the next lesson in side cards and a live countdown.
- Rebuilt the shelter/air-raid page as the red companion of the normal page.
- Added lesson number and lesson/break countdown to shelter mode.
- Added compact Kropyvnytskyi weather to normal, break and shelter modes.
- Made every subject name use one consistent size.
- Reduced teacher typography relative to subject typography.
- Kept rooms visually prominent and groups in separate badges.
- Removed the technical footer from presentation pages.

## Minute of silence
- Removed the playback caption.
- Removed the lower-left technical line.
- Reworked the candle with warm radial glow, wax shading, wick and layered flame.

## Admin / migration
- Kept `admin_settings_v1` and all existing keys.
- Kept package identity for in-place upgrades.
- Existing settings are not overwritten.
- Added default GitHub manifest and Google Apps Script URLs only when fields are empty.
- `BUILD_AND_INSTALL.ps1` uses `pm install -r -t`; `-FreshInstall` is guarded behind explicit confirmation.

## Data / sync
- Preserved all four production schedule files.
- Preserved `01.09.2026 = ЧИСЕЛЬНИК`.
- Preserved UID 81 alert monitoring.
- Preserved alarm and minute-of-silence audio.
- Regenerated the sample-server manifest with SHA-256 hashes.

## References
Final exact 1920×1080 references:
- `docs/mockups/normal_schedule_final_1920x1080.png`
- `docs/mockups/break_schedule_final_1920x1080.png`
- `docs/mockups/shelter_schedule_final_1920x1080.png`
- `docs/mockups/minute_silence_final_reference_1920x1080.png`
