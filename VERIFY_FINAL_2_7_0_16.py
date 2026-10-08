from pathlib import Path
import hashlib
import json
import re
import struct

root = Path(__file__).resolve().parent
java = root / "app/src/main/java/ua/edu/cunl/tv"
view = (java / "ui/SignageView.java").read_text(encoding="utf-8")
admin = (java / "admin/AdminDialog.java").read_text(encoding="utf-8")
main = (java / "MainActivity.java").read_text(encoding="utf-8")
gradle = (root / "app/build.gradle").read_text(encoding="utf-8")
manifest_xml = (root / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")

assert "versionCode 56" in gradle
assert "versionName '2.7.0.16'" in gradle
assert "applicationId 'ua.edu.cunl.tv'" in gradle
assert 'PREFS = "admin_settings_v1"' in admin

# Final TV page behavior retained.
assert "drawBreakCenter" in view
assert "ДО ПОЧАТКУ " in view
assert "ПОВІТРЯНА ТРИВОГА" in view
assert "drawRaidCenter" in view
assert "ПОГОДА • КРОПИВНИЦЬКИЙ" in view
assert "Фонограма відтворюється один раз" not in view
assert "drawFooter(canvas" not in view
assert "private void drawFooter" not in view
assert "subjectSize =" in view and "teacherSize =" in view

# Remote admin defaults retained.
assert "DEFAULT_MANIFEST_URL" in admin
assert "raw.githubusercontent.com/salenko93-hash/lyceumtv-updates/main/sample-server/content_manifest.json" in admin
assert "DEFAULT_SHEETS_URL" in admin
assert "AKfycbwoo2SlQh3ojGRycG9EmUMhAqOgIhMlN9lCw6BzVDGlihVWAYonx1YTmUE8ap7EDKgSEA" in admin
assert "AdminDialog.KEY_MANIFEST_URL" in main
assert "AdminDialog.KEY_SHEETS_URL" in main

# Android TV launcher branding.
assert 'android:icon="@mipmap/ic_launcher"' in manifest_xml
assert 'android:roundIcon="@mipmap/ic_launcher"' in manifest_xml
assert 'android:banner="@drawable/tv_banner"' in manifest_xml
assert 'android.intent.category.LEANBACK_LAUNCHER' in manifest_xml

def png_size(path):
    data = path.read_bytes()[:24]
    assert data[:8] == b"\x89PNG\r\n\x1a\n", f"Not PNG: {path}"
    return struct.unpack(">II", data[16:24])

branding = {
    "mipmap-mdpi/ic_launcher.png": (48, 48),
    "mipmap-hdpi/ic_launcher.png": (72, 72),
    "mipmap-xhdpi/ic_launcher.png": (96, 96),
    "mipmap-xxhdpi/ic_launcher.png": (144, 144),
    "mipmap-xxxhdpi/ic_launcher.png": (192, 192),
    "drawable-nodpi/tv_banner.png": (320, 180),
}
res = root / "app/src/main/res"
for rel, expected in branding.items():
    p = res / rel
    assert p.is_file(), rel
    assert png_size(p) == expected, (rel, png_size(p), expected)

# App/server data remains byte-identical.
assets = root / "app/src/main/assets"
server = root / "sample-server"
files = [
    "schedule_numerator.json", "schedule_denominator.json",
    "shelter_numerator.json", "shelter_denominator.json",
    "calendar.json", "content.json"
]
for name in files:
    assert (assets / name).is_file(), name
    assert (server / name).is_file(), name
    assert hashlib.sha256((assets / name).read_bytes()).hexdigest() == \
           hashlib.sha256((server / name).read_bytes()).hexdigest(), name

remote_manifest = json.loads((server / "content_manifest.json").read_text(encoding="utf-8"))
for name in files[:4]:
    assert name in remote_manifest["schedules"]

# Exact 1920x1080 references checked without Pillow.
def image_size(path):
    data = path.read_bytes()
    if data[:8] == b"\x89PNG\r\n\x1a\n":
        return struct.unpack(">II", data[16:24])
    raise AssertionError(f"Unsupported image: {path}")

mockups = [
    "normal_schedule_final_1920x1080.png",
    "break_schedule_final_1920x1080.png",
    "shelter_schedule_final_1920x1080.png",
    "minute_silence_final_reference_1920x1080.png",
]
for name in mockups:
    p = root / "docs/mockups" / name
    assert p.is_file(), name
    assert image_size(p) == (1920, 1080), (name, image_size(p))

print("PASS: LyceumTV 2.7.0.16 final TV design verified.")
print("PASS: lesson/break/shelter/silence pages and 1920x1080 references present.")
print("PASS: Android TV launcher icon + 320x180 banner integrated.")
print("PASS: package ID, admin prefs, GitHub/Apps Script defaults preserved.")
print("PASS: app/server schedule data remains byte-identical.")
