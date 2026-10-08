# LyceumTV 2.6.5 — schedule source mapping

The application schedule assets were generated directly from the four supplied XLSX workbooks.
No OCR was used.

## Normal mode

- Numerator:
  `Розклад 10-11 Чисельник(2).xlsx`
- Denominator:
  `Розклад 10-11 Знаменник(1).xlsx`

Assets:

- `schedule_numerator.json`
- `schedule_denominator.json`

## Air-raid / shelter mode

- Numerator shelter:
  `РОЗКЛАД  10–11 КЛАСІВ — ЧИСЕЛЬНИК УКРИТТЯ.xlsx`
- Denominator shelter:
  `РОЗКЛАД 10–11 КЛАСІВ — ЗНАМЕННИК УКРИТТЯ_1.xlsx`

Assets:

- `shelter_numerator.json`
- `shelter_denominator.json`

## Workbook layout

- A: class
- B: lesson time
- C:F: Monday
- G:J: Tuesday
- K:N: Wednesday
- O:R: Thursday
- S:V: Friday
- W:Z: Saturday

Each four-column day group contains:

1. lesson number
2. subject/group
3. room or shelter location
4. teacher

Continuation rows are preserved as additional entries for the same lesson.

## Runtime behavior

Normal state:

```text
NUMERATOR   -> schedule_numerator.json
DENOMINATOR -> schedule_denominator.json
```

Air raid:

```text
NUMERATOR   -> shelter_numerator.json
DENOMINATOR -> shelter_denominator.json
```

The current lesson is shown during lessons. During a break, the next lesson is shown.

When alerts.in.ua returns `A` or `P`, the display enters `AIR_RAID` and immediately shows
the corresponding shelter timetable.

When alerts.in.ua returns `N` after an active alert, LyceumTV shows ALL CLEAR for
15 seconds and then returns to the normal timetable.
