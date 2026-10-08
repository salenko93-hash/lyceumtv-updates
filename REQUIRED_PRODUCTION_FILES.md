# Required production files — 2.7.0.14

All production-critical files are included in this package:

- `app/src/main/assets/schedule_numerator.json`
- `app/src/main/assets/schedule_denominator.json`
- `app/src/main/assets/shelter_numerator.json`
- `app/src/main/assets/shelter_denominator.json`
- `app/src/main/assets/calendar.json`
- `app/src/main/res/raw/minute_silence.mp3`
- `app/src/main/res/raw/trivoga.mp3`
- `app/src/main/res/drawable-nodpi/lyceum_logo.png`
- `app/src/main/res/drawable-nodpi/lyceum_logo_white.png`

The original input XLSX and MP3 files are preserved under `source-data/`.

Run `python .\VERIFY_SOURCE_UID81.py` before the Gradle build.

Approved design references:

- `docs/mockups/normal_schedule_approved.png`
- `docs/mockups/shelter_schedule_approved.png`
