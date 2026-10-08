# LyceumTV 2.7.0.10 — Reference-style screen

This release replaces the 2.7.0.9 Premium Broadcast composition with the layout
requested from the supplied reference image.

## Normal screen
- light schedule tables: 10-А…10-Г left, 11-А…11-Г right;
- logo and status on the center axis;
- explicit `ЧИСЕЛЬНИК` or `ЗНАМЕННИК`;
- day, date and large clock;
- clear `1 УРОК`, `2 УРОК`, `ПЕРЕРВА`, `ПІСЛЯ УРОКІВ` states.

## Alert states
- AIR_RAID keeps the same composition but uses a red visual system and the
  shelter timetable.
- ALL_CLEAR uses a green screen for 15 seconds.
- Admin remains available during AIR_RAID.

## Break announcements
Announcements are shown only on real timetable breaks, never during AIR_RAID,
ALL_CLEAR, minute-of-silence, lessons, holidays or after-school mode.

Rotation rule:
`30 seconds schedule -> 15 seconds announcement`, with the last 60 seconds
before the next lesson reserved for the schedule.

If no active announcement is synchronized, nothing replaces the schedule.
