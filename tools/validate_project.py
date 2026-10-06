#!/usr/bin/env python3
from pathlib import Path
import json, re, sys, hashlib

root = Path(__file__).resolve().parents[1]
assets = root / "app/src/main/assets"
src = root / "app/src/main/java/ua/edu/cunl/lyceummobile"
canonical = {"10-А","10-Б","10-В","10-Г","11-А","11-Б","11-В","11-Г"}
names = [
    "schedule_numerator.json",
    "schedule_denominator.json",
    "shelter_numerator.json",
    "shelter_denominator.json",
]

for name in names:
    obj = json.loads((assets/name).read_text(encoding="utf-8"))
    assert len(obj.get("bellSchedule", [])) == 8, name
    assert obj.get("days"), name
    for day, lessons in obj["days"].items():
        assert day in {"MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY"}
        for lesson, classes in lessons.items():
            assert lesson.isdigit()
            for class_name, entries in classes.items():
                assert class_name in canonical, (name, class_name)
                for entry in entries:
                    assert all(k in entry for k in ("subject","room","teacher"))
    print("OK", name)

calendar = json.loads((assets/"calendar.json").read_text(encoding="utf-8"))
assert "daysOff" in calendar and "ranges" in calendar

build = (root/"app/build.gradle.kts").read_text(encoding="utf-8")
assert 'applicationId = "ua.edu.cunl.lyceummobile"' in build
assert "minSdk = 26" in build
assert "targetSdk = 35" in build
assert 'versionName = "1.0-redmi15-v9"' in build

manifest = (root/"app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
assert "android.permission.INTERNET" in manifest
assert 'android:usesCleartextTraffic="false"' in manifest

all_code = "\n".join(p.read_text(encoding="utf-8") for p in src.rglob("*.kt"))
assert "active_air_raid_alerts/81.json" in all_code
assert "AndroidKeyStore" in all_code
assert "MessageDigest.getInstance(\"SHA-256\")" in all_code
assert "schedule_numerator.json" in all_code
assert "shelter_denominator.json" in all_code
assert "UkraineAlarm" not in all_code
assert "http://" not in all_code
assert "Bearer " in all_code
assert "LyceumMobile" in all_code

screens = (src/"ui/Screens.kt").read_text(encoding="utf-8")
for symbol in (
    "DashboardScreen", "TimetableScreen", "AnnouncementsScreen",
    "MinuteSilenceScreen", "EmergencyScreen", "AllClearScreen", "SettingsScreen"
):
    assert f"fun {symbol}" in screens, symbol

raw = root/"app/src/main/res/raw/metronome_tick.wav"
assert raw.is_file() and raw.stat().st_size > 100

print("OK package/minSdk/targetSdk, UID81, Keystore, SHA-256, HTTPS-only, UI screens.")
print("Kotlin files:", len(list(src.rglob("*.kt"))))
print("Metronome SHA256:", hashlib.sha256(raw.read_bytes()).hexdigest())

assert 'sourceCompatibility = JavaVersion.VERSION_17' in build
assert 'targetCompatibility = JavaVersion.VERSION_17' in build
assert 'jvmTarget = "17"' in build
assert 'jvmToolchain(17)' in build

ps1 = (root/"BUILD_AND_INSTALL.ps1").read_text(encoding="utf-8-sig")
assert "JDK 17" in ps1
print("OK strict JDK 17 toolchain.")

settings = (root/"settings.gradle.kts").read_text(encoding="utf8")
assert "mavenCentral()" not in settings

settings = (root/"settings.gradle.kts").read_text(encoding="utf8")
assert "mavenCentral()" not in settings
assert "repo.maven.apache.org" not in settings
assert "repo1.maven.org" not in settings
assert "https://cache-redirector.jetbrains.com/maven-central" in settings
print("OK Maven Central routed through JetBrains cache redirector.")

settings = (root/"settings.gradle.kts").read_text(encoding="utf8")
wrapper_props = (root/"gradle/wrapper/gradle-wrapper.properties").read_text(encoding="utf8")
wrapper_bat = (root/"gradlew.bat").read_text(encoding="utf-8-sig")
assert "repo.maven.apache.org" not in settings
assert "repo1.maven.org" not in settings
assert "https://cache-redirector.jetbrains.com/maven-central" in settings
assert "gradlePluginPortal()" not in settings
assert "gradle-8.10.2-bin.zip" in wrapper_props
assert (root/"gradle/wrapper/gradle-wrapper.jar").is_file()
assert (root/"CHECK_NETWORK.ps1").is_file()
assert "GradleWrapperMain" in wrapper_bat
print("OK FINAL wrapper/network checks.")

ps1 = (root/"BUILD_AND_INSTALL.ps1").read_text(encoding="utf-8-sig")
assert "Get-JavaMajorVersion" in ps1
assert "System.Diagnostics.ProcessStartInfo" in ps1
assert "Microsoft.OpenJDK.17" in ps1
assert "Amazon Corretto" in ps1
assert "-eq 17" in ps1
print("OK Windows PowerShell 5.1-safe JDK 17 selector.")

# v5/v6 regression checks
lyceum_app = (src/"ui/LyceumApp.kt").read_text(encoding="utf-8")
assert "import androidx.compose.runtime.saveable.rememberSaveable" in lyceum_app
assert "import androidx.compose.runtime.rememberSaveable" not in lyceum_app
assert not (root/"gradlew.bat").read_bytes().startswith(b"\xef\xbb\xbf")
ps1_bytes = (root/"BUILD_AND_INSTALL.ps1").read_bytes()
assert ps1_bytes.startswith(b"\xef\xbb\xbf") and not ps1_bytes.startswith(b"\xef\xbb\xbf\xef\xbb\xbf")
assert "org.gradle.wrapper.GradleWrapperMain --no-daemon clean test :app:assembleDebug" in ps1

installer = (root/"INSTALL_EXISTING_APK.ps1").read_text(encoding="utf-8-sig")
for marker in ("INSTALL_FAILED_UPDATE_INCOMPATIBLE", "INSTALL_FAILED_USER_RESTRICTED", "--no-streaming", "-ReplaceExisting", "pm','path"):
    assert marker in installer, marker
assert (root/"START_INSTALL_EXISTING_APK.cmd").is_file()
assert (root/"START_REPLACE_OLD_AND_INSTALL.cmd").is_file()
assert "Безпека · UID 81" not in screens
assert "Пошук учителя" in screens
assert "teacherMatches" in all_code
assert "teacherDayStatus" in all_code
assert "Сьогодні уроків немає" in screens
print("OK v8 teacher-search / dashboard-security-card regressions.")

# v9 PC remote URL configuration checks
main_activity = (src/"MainActivity.kt").read_text(encoding="utf-8")
assert "pc_google_url_b64" in main_activity
assert "pc_github_manifest_url_b64" in main_activity
assert "raw.githubusercontent.com" in main_activity
assert "script.google.com" in main_activity
assert 'android:launchMode="singleTop"' in manifest
remote_ps1 = (root/"SET_REMOTE_URLS.ps1").read_text(encoding="utf-8-sig")
assert "GoogleAppsScriptUrl" in remote_ps1
assert "GithubManifestUrl" in remote_ps1
assert "pc_google_url_b64" in remote_ps1
assert "pc_github_manifest_url_b64" in remote_ps1
assert (root/"START_SET_REMOTE_URLS.cmd").is_file()
print("OK v9 PC-to-phone Google Apps Script / GitHub Raw configuration.")
