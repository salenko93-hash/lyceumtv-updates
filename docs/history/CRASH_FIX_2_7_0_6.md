# LyceumTV 2.7.0.7 — Android TV startup crash fix

## Fixed startup crash

On older Android TV tzdata, `Europe/Kyiv` is not registered even though the
equivalent historical IANA identifier `Europe/Kiev` is available. Version
2.7.0.5 used `ZoneId.of("Europe/Kyiv")` during class initialization, causing:

`java.time.zone.ZoneRulesException: Unknown time-zone ID: Europe/Kyiv`

Version 2.7.0.7 resolves Kyiv time centrally:
1. try `Europe/Kyiv`;
2. fallback to `Europe/Kiev` for older Android TV;
3. fallback to the device time zone only if neither identifier is available.

The visible label remains `Europe/Kyiv`.

## Branding

The supplied logo is bundled as `res/drawable-nodpi/lyceum_logo.png`.
The application label is `Центральноукраїнський науковий ліцей` and the full
name displayed in the UI is:

`Центральноукраїнський науковий ліцей Кіровоградської обласної ради`

## Version

- versionCode: 46
- versionName: 2.7.0.7
- debug: 2.7.0.7-debug

Build first:

```powershell
.\BUILD_AND_INSTALL.ps1 -BuildOnly
```

Then, outside an active emergency:

```powershell
.\BUILD_AND_INSTALL.ps1
```

Do not use `-FreshInstall`.
