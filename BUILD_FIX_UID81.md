# LyceumTV 2.7.0.1 — FIXED build for UID 81

The Windows compiler errors were caused by `MainActivity` calling two missing
methods on `SignageView` (`setScreenProfile` twice and `setClockStatus` once).

This revision implements both methods, with accepted profiles
`AUTO`, `FHD`, `HD`, and `OTHER`, and retains the NTP warning. No alarm
logic, schedules, API token storage, remote update behavior or IP was changed.

**Unchanged deployment:**
- alerts.in.ua UID: `81`, Kropyvnytskyi district.
- versionCode: `41`
- versionName: `2.7.0.1` (`2.7.0.1-debug` on the debug APK).
- Default TV ADB address: `192.168.5.77:5555`.

## Option A — FULL package

Unpack the FIXED FULL ZIP into a fresh folder; open PowerShell there.

```powershell
.\BUILD_AND_INSTALL.ps1 -BuildOnly
```

If you see `BUILD SUCCESSFUL`, and only when no alert is active:

```powershell
.\BUILD_AND_INSTALL.ps1
```

Do not use `-FreshInstall`: that would erase your existing encrypted API token.

## Option B — HOTFIX

If you already have the old 2.7.0.1 source folder, extract
`LyceumTV_2_7_0_1_UID81_BUILD_HOTFIX.zip` directly over the project root,
agreeing to replace files. The HOTFIX contains `SignageView.java`,
the updated regression test, and this note.

Then run the same two build commands above.

This release includes local source tests and a Java compilation check using
Android API stubs. Complete Android Gradle compilation must be executed on
your Windows PC, which has the Android SDK.
