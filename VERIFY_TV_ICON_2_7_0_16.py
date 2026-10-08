from pathlib import Path
import re, struct, sys

ROOT = Path(__file__).resolve().parent

def png_size(path):
    data = path.read_bytes()[:24]
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise AssertionError(f"Not a PNG: {path}")
    return struct.unpack(">II", data[16:24])

manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
gradle = (ROOT / "app/build.gradle").read_text(encoding="utf-8")

assert 'android:icon="@mipmap/ic_launcher"' in manifest
assert 'android:roundIcon="@mipmap/ic_launcher"' in manifest
assert 'android:banner="@drawable/tv_banner"' in manifest
assert 'android.intent.category.LEANBACK_LAUNCHER' in manifest
assert "applicationId 'ua.edu.cunl.tv'" in gradle
assert "versionName '2.7.0.16'" in gradle
assert re.search(r"versionCode\s+56\b", gradle)

expected = {
    "mipmap-mdpi/ic_launcher.png": (48,48),
    "mipmap-hdpi/ic_launcher.png": (72,72),
    "mipmap-xhdpi/ic_launcher.png": (96,96),
    "mipmap-xxhdpi/ic_launcher.png": (144,144),
    "mipmap-xxxhdpi/ic_launcher.png": (192,192),
    "drawable-nodpi/tv_banner.png": (320,180),
}

for rel, size in expected.items():
    p = ROOT / "app/src/main/res" / rel
    assert p.exists(), f"Missing {p}"
    assert png_size(p) == size, f"{rel}: expected {size}, got {png_size(p)}"

admin = ROOT / "app/src/main/java/ua/edu/cunl/tv/admin/AdminDialog.java"
if admin.exists():
    text = admin.read_text(encoding="utf-8")
    assert "admin_settings_v1" in text, "admin prefs identity changed"

print("PASS: LyceumTV 2.7.0.16 Android TV icon/banner verified.")
print("PASS: launcher icon density set + 320x180 TV banner present.")
print("PASS: package IDs and admin preferences identity preserved.")
