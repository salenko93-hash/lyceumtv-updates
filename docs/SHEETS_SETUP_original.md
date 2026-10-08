# Google Sheets 2.7.0.1

See `DEPLOY_REMOTE_UPDATES.md` option 2 and deploy the current
`apps-script/Code.gs`. In addition to old Announcements, Events and Schedule,
add `Substitutions` sheet with columns:
`date,week,day,lesson,class,scope,active,entries_json`.

The resulting Apps Script Web App HTTPS URL is used only in
`GITHUB + GOOGLE SHEETS` mode. For local NAS mode, publish
`sample-server/content.json` alongside the four timetable JSONs.
