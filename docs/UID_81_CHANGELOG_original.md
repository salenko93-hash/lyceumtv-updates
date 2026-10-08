# UID 81 release: LyceumTV 2.7.0.1

- One fixed alerts.in.ua UID: `81` (Kropyvnytskyi district).
- Same TV IP: `192.168.5.77:5555`.
- Upgrade versionCode 40 -> 41; versionName 2.7.0 -> 2.7.0.1.
- Old UID 761 API state and Last-Modified are discarded by `AlertsInUaStore` on first launch.
- Encrypted API token, administrator settings, remote schedules, and all four bundled schedules are preserved.
- Both update sources (GitHub + Google Sheets and local NAS) stay available.
- One automatic alarm API; no official-notification fallback.
- Tests and API test/diagnostics scripts updated to UID 81.
- All other 2.7.0 functionality unchanged.

Caution: Kropyvnytskyi district UID 81 covers more than the
Kropyvnytskyi territorial community UID 761. Both A and P are
treated as an active alarm. Only rely on official emergency channels
for safety-critical decisions.

To build on Windows:

```powershell
.\BUILD_AND_INSTALL.ps1 -BuildOnly
.\BUILD_AND_INSTALL.ps1
```

Do not use `-FreshInstall` if you wish to preserve your API token.
