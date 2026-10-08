# DATA IMPORT REPORT — LyceumTV 2.7.0.13

Generated from the latest user-supplied XLSX workbooks.

## schedule_numerator.json
- source: `Розклад 10-11 Чисельник(6).xlsx`
- structured entries: **481**
- continuation rows with inherited merged subject: **1**
- orphan rows skipped: **0**
- bell periods: **8** (`08:00`–`15:15`)

## schedule_denominator.json
- source: `Розклад 10-11 Знаменник(5).xlsx`
- structured entries: **486**
- continuation rows with inherited merged subject: **1**
- orphan rows skipped: **0**
- bell periods: **8** (`08:00`–`15:15`)

## shelter_numerator.json
- source: `РОЗКЛАД  10–11 КЛАСІВ — ЧИСЕЛЬНИК УКРИТТЯ(4).xlsx`
- structured entries: **496**
- continuation rows with inherited merged subject: **14**
- orphan rows skipped: **0**
- bell periods: **8** (`08:00`–`15:15`)

## shelter_denominator.json
- source: `РОЗКЛАД 10–11 КЛАСІВ — ЗНАМЕННИК УКРИТТЯ(3).xlsx`
- structured entries: **493**
- continuation rows with inherited merged subject: **9**
- orphan rows skipped: **0**
- bell periods: **8** (`08:00`–`15:15`)

## Week parity
- `01.09.2026` is in a **ЧИСЕЛЬНИК** week.
- `07.10.2026` is in a **ЗНАМЕННИК** week.
- `12.10.2026` starts the next **ЧИСЕЛЬНИК** week.

## Audio
- `03 хвилина мовчання(3).mp3` → `app/src/main/res/raw/minute_silence.mp3` — 79.536 s.
- `TRIVOGA.mp3` → `app/src/main/res/raw/trivoga.mp3` — 50.208 s, one-shot at AIR_RAID entry.

## Notes
- Source text is preserved except whitespace normalization.
- When Excel uses a merged subject cell for a second group, the parser carries forward the subject within the same lesson.
- A genuinely blank subject in the source is not invented or corrected.
