# LyceumTV 2.7.0.13 — REAL SCHEDULE + SHELTER + AUDIO

## Imported schedule sources
- Розклад 10-11 Чисельник(6).xlsx → `schedule_numerator.json`
- Розклад 10-11 Знаменник(5).xlsx → `schedule_denominator.json`
- РОЗКЛАД  10–11 КЛАСІВ — ЧИСЕЛЬНИК УКРИТТЯ(4).xlsx → `shelter_numerator.json`
- РОЗКЛАД 10–11 КЛАСІВ — ЗНАМЕННИК УКРИТТЯ(3).xlsx → `shelter_denominator.json`

## Import results
- normal numerator: 481 structured entries
- normal denominator: 486 structured entries
- shelter numerator: 496 structured entries
- shelter denominator: 493 structured entries
- orphan rows: 0
- classes: 10-А, 10-Б, 10-В, 10-Г, 11-А, 11-Б, 11-В, 11-Г
- days: Monday–Saturday
- bell schedule from workbooks: lessons 1–8, 08:00–15:15

Merged/continuation Excel cells are handled by carrying the subject into a continuation
row when only its room/teacher is present. No lesson rows were discarded.

## Week rule
The week containing 01.09.2026 is ЧИСЕЛЬНИК. Alternation is calculated by the Monday
containing that date, so 31.08–06.09.2026 is numerator.

## Audio
- `03 хвилина мовчання(3).mp3` → `res/raw/minute_silence.mp3` (79.536 s)
- `TRIVOGA.mp3` → `res/raw/trivoga.mp3` (50.208 s)
- TRIVOGA is one-shot on transition into AIR_RAID and is never looped.
- Alarm sound can be enabled/disabled and volume can be set in Admin.
- Alarm audio is stopped on all-clear/background/destroy.

## Existing 2.7.0.12 features retained
- white cropped TV logo
- Kropyvnytskyi live weather
- countdown to lesson end / break end
- normal/shelter schedule screens
- alerts.in.ua UID 81
- admin access during active alert
