# LyceumTV 2.7.0.12 — schedule UX update

## Week rule
- `01.09.2026` belongs to the **ЧИСЕЛЬНИК** week.
- The reference week starts on Monday `31.08.2026`.
- Every following Monday alternates numerator/denominator.
- Therefore `05.10.2026–11.10.2026` is **ЗНАМЕННИК**.

## Main TV screen
The center column is now a clear information hierarchy:
1. white/cropped lyceum logo;
2. numerator/denominator pill;
3. weekday and date;
4. large Kyiv time;
5. phase + live countdown;
6. Kropyvnytskyi weather.

During a lesson the countdown is calculated from the exact `bellSchedule.end`.
During a break it is calculated to the next lesson `bellSchedule.start`.
The screen is invalidated every second, so both countdown modes update live.

## Weather
`WeatherRepository` uses Open-Meteo via HTTPS for fixed Kropyvnytskyi coordinates.
No location permission and no API key are required. The screen shows:
- current temperature;
- Ukrainian text condition derived from WMO weather code;
- apparent temperature.

Refresh: 10 minutes. Retry after network failure: 2 minutes. If previously loaded
weather exists, a transient refresh failure does not erase the last useful reading.

## Logo
The supplied original `lyceum_logo.png` is preserved byte-for-byte for the launcher.
A separate `lyceum_logo_white.png` is generated for TV/admin use:
- RGB converted to white;
- alpha preserved;
- excessive transparent padding cropped;
- renderer adds a low-opacity dark halo.

## Alert priority
Weather is intentionally not drawn in the AIR_RAID center state, so safety messaging
retains visual priority. The shelter timetable remains available around the center.
