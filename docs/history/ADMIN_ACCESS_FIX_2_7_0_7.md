# LyceumTV 2.7.0.7 — Android TV admin access fix

- versionCode 47 / versionName 2.7.0.7
- Hold OK/ENTER for ~0.9 s: the admin dialog opens from a timed key-down handler.
- MENU or SETTINGS also opens admin when no alarm is active.
- During a confirmed/manual alarm the shelter timetable has priority:
  admin is blocked and any open admin dialog is dismissed immediately.
- Admin content is inside a ScrollView for TV screens.
- API token field remains in the admin dialog.
- Existing UID 81, schedules, calendar, logo, school name and silence MP3 are preserved.
