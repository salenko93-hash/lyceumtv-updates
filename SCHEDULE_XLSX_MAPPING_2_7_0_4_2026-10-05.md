# LyceumTV 2.7.0.4 — schedule replacement 2026-10-05-schedule-01

The four bundled and remote schedule JSON files were regenerated directly
from the four supplied XLSX workbooks. No OCR or guessed timetable data was used.

## Normal mode

- Numerator source: `Розклад 10-11 Чисельник(3).xlsx`
  -> `schedule_numerator.json`
- Denominator source: `Розклад 10-11 Знаменник(2).xlsx`
  -> `schedule_denominator.json`

## Air-raid / shelter mode

- Numerator shelter source: `РОЗКЛАД  10–11 КЛАСІВ — ЧИСЕЛЬНИК УКРИТТЯ(1).xlsx`
  -> `shelter_numerator.json`
- Denominator shelter source: `РОЗКЛАД 10–11 КЛАСІВ — ЗНАМЕННИК УКРИТТЯ.xlsx`
  -> `shelter_denominator.json`

## XLSX mapping

- A: class
- B: lesson time
- C:F: Monday
- G:J: Tuesday
- K:N: Wednesday
- O:R: Thursday
- S:V: Friday
- W:Z: Saturday

Each day uses four columns: lesson number, subject/group, room/shelter
location, teacher. Continuation rows are retained as additional entries for
the same class/day/lesson.

The current LyceumTV contract contains 8 bells (08:00–15:15).
The numerator-shelter workbook contains an empty 9th template row
(15:25–16:10) with no subject, room or teacher data, so it is intentionally
not emitted. No actual lesson data is discarded.

Remote bundle version: `2026-10-05-schedule-01`.
