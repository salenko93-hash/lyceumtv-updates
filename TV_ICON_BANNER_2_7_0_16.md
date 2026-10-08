# LyceumTV 2.7.0.16 — Android TV icon and banner

This release adds launcher branding for Android TV while keeping the same
application IDs and the same admin SharedPreferences identity.

## Android resources

- `@mipmap/ic_launcher` — launcher icon in mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi.
- `@drawable/tv_banner` — 320×180 Android TV banner.
- `android:banner="@drawable/tv_banner"` is set on `<application>`.
- `LEANBACK_LAUNCHER` remains enabled.
- `applicationId` remains `ua.edu.cunl.tv` (`ua.edu.cunl.tv.debug` for debug).

## Settings preservation

Installing with `adb install -r -t` or `adb install --no-streaming -r -t`
updates the existing app and preserves `admin_settings_v1` as long as the
APK signature is compatible. Do not uninstall the old app if you want to
preserve app data.
