# Premium Broadcast UI / state specification — 2.7.0.8

## NORMAL — schedule board

Visual direction: premium broadcast control-room aesthetic. Dark navy/graphite gradient,
thin luminous blue rails, glass-like cards, high-contrast white typography and the supplied
lyceum logo. The header contains the full institution name and a compact LIVE indicator.

The main status area shows:
- schedule mode;
- ЧИСЕЛЬНИК / ЗНАМЕННИК;
- lesson/break phase;
- large HH:mm clock with seconds;
- Ukrainian weekday and date.

Eight class rows are rendered as 2 columns × 4 cards for 10-А through 11-Г.

## AIR_RAID — red shelter schedule

The whole screen changes to a dark red broadcast gradient. The top status card shows:
`ПОВІТРЯНА ТРИВОГА` and `НЕГАЙНО ПРОЙДІТЬ В УКРИТТЯ`.

The 2×4 class grid switches to the shelter timetable. The footer records alerts.in.ua UID 81
or manual source plus the start time. Holding OK still opens admin.

## ALL_CLEAR — green

The whole screen changes to green with a large check mark,
`ВІДБІЙ ПОВІТРЯНОЇ ТРИВОГИ`, current time and a live countdown. After 15 seconds the normal
schedule board resumes.

## SILENCE

Dark neutral background, gold accent and candle motif. The original supplied MP3 plays once.
A real confirmed/manual alert interrupts the audio immediately.

## ADMIN

Dark graphite 90%-screen panel optimized for D-pad focus. Sections:
- alert/API;
- academic week;
- synchronization/sources;
- display;
- minute of silence.

The panel remains open and usable during AIR_RAID. It shows the active state but does not
hide or modify the confirmed API result unless an explicit administrator action is taken.
