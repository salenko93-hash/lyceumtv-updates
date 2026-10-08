# LyceumTV 2.7.0.11 — structured schedule cards + weather + supplied logo

## What changed

The reference-style screen now keeps each lesson field visually separate:

- class;
- lesson number;
- subject;
- room (`КАБ.`);
- teacher;
- explicit subgroup markers (`гр.1`, `гр.2`, `група`, `підгрупа`) as separate blocks.

The renderer displays up to two source entries for the same class/lesson as two independent blocks.
This matches the supplied production JSONs, where the maximum number of entries for one
class/day/lesson is two. Empty fields remain empty/dash and are not guessed.

## Data check

Across the four production schedules:

| file | entries | multi-entry lessons | explicit group-marked entries | max entries per lesson |
|---|---:|---:|---:|---:|
| schedule_numerator.json | 481 | 177 | 355 | 2 |
| schedule_denominator.json | 486 | 181 | 362 | 2 |
| shelter_numerator.json | 496 | 191 | 358 | 2 |
| shelter_denominator.json | 493 | 189 | 362 | 2 |

## Weather

Admin contains `ПОГОДА • КРОПИВНИЦЬКИЙ`.
It opens the current weather search in the installed TV browser:

`https://www.google.com/search?q=weather+Kropyvnytskyi`

No stale weather values are embedded in the APK.

## Logo

The supplied logo is preserved byte-for-byte as
`app/src/main/res/drawable-nodpi/lyceum_logo.png`
and is used on the main screen and at the top of the admin panel.

SHA-256:

`fb0482cf1c6ecdac0e51e654a385e625e6b051153b20a87cf855640e8b3c83a1`

## Version

- versionCode: 51
- versionName: 2.7.0.11
- debug package: `ua.edu.cunl.tv.debug`
- alerts.in.ua UID: 81
