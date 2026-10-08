# Admin settings preservation — LyceumTV 2.7.0.15

This release is designed as an in-place upgrade.

## Preserved Android identities
- debug package: `ua.edu.cunl.tv.debug`
- admin preferences file: `admin_settings_v1`
- secure alerts token store: `alerts_token_secure_v1`

## Preserved admin values
Existing values remain unchanged for:
- numerator start date;
- break announcements;
- minute-of-silence enabled state and volume;
- alarm sound enabled state and volume;
- GitHub/NAS Manifest URL;
- Google Apps Script Web App URL;
- screen profile.

The application writes a default only when a value is missing or blank.

## Safe update
Use:

```powershell
.\BUILD_AND_INSTALL.ps1
```

The script installs with `pm install -r -t`.

Do not use:

```powershell
.\BUILD_AND_INSTALL.ps1 -FreshInstall -ConfirmDataLoss
```

unless deleting the old application data is intentional.

## Embedded source defaults
Manifest URL:
`https://raw.githubusercontent.com/salenko93-hash/lyceumtv-updates/main/sample-server/content_manifest.json`

Google Apps Script:
`https://script.google.com/macros/s/AKfycbwoo2SlQh3ojGRycG9EmUMhAqOgIhMlN9lCw6BzVDGlihVWAYonx1YTmUE8ap7EDKgSEA/exec`
