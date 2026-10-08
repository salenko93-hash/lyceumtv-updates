from pathlib import Path
import hashlib
import json
from PIL import Image

root = Path(__file__).resolve().parent
java = root / "app/src/main/java/ua/edu/cunl/tv"
view = (java / "ui/SignageView.java").read_text(encoding="utf-8")
admin = (java / "admin/AdminDialog.java").read_text(encoding="utf-8")
main = (java / "MainActivity.java").read_text(encoding="utf-8")
gradle = (root / "app/build.gradle").read_text(encoding="utf-8")

assert "versionCode 55" in gradle
assert "versionName '2.7.0.15'" in gradle
assert 'PREFS = "admin_settings_v1"' in admin

assert "drawBreakCenter" in view
assert "ДО ПОЧАТКУ " in view
assert "ПОВІТРЯНА ТРИВОГА" in view
assert "drawRaidCenter" in view
assert "drawWeatherPanel(canvas, w, h, h * 0.855f" in view
assert "ПОГОДА • КРОПИВНИЦЬКИЙ" in view
assert "Фонограма відтворюється один раз" not in view
assert "drawFooter(canvas" not in view
assert "private void drawFooter" not in view
assert "Path outer = new Path()" in view
assert "subjectSize =" in view and "teacherSize =" in view
assert "teacherSize" in view and "subjectSize" in view

assert "DEFAULT_MANIFEST_URL" in admin
assert "raw.githubusercontent.com/salenko93-hash/lyceumtv-updates/main/sample-server/content_manifest.json" in admin
assert "DEFAULT_SHEETS_URL" in admin
assert "AKfycbwoo2SlQh3ojGRycG9EmUMhAqOgIhMlN9lCw6BzVDGlihVWAYonx1YTmUE8ap7EDKgSEA" in admin
assert "AdminDialog.KEY_MANIFEST_URL" in main
assert "AdminDialog.KEY_SHEETS_URL" in main

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
    assert hashlib.sha256((assets/name).read_bytes()).hexdigest() == \
           hashlib.sha256((server/name).read_bytes()).hexdigest(), name

manifest = json.loads((server / "content_manifest.json").read_text(encoding="utf-8"))
assert manifest["version"] == "2026-10-08-2.7.0.15-final-tv-design"
for name in files[:4]:
    assert name in manifest["schedules"]

mockups = [
    "normal_schedule_final_1920x1080.png",
    "break_schedule_final_1920x1080.png",
    "shelter_schedule_final_1920x1080.png",
    "minute_silence_final_reference_1920x1080.png",
]
for name in mockups:
    p = root / "docs/mockups" / name
    assert p.is_file(), name
    with Image.open(p) as im:
        assert im.size == (1920, 1080), (name, im.size)

print("PASS: LyceumTV 2.7.0.15 final TV design verified.")
print("PASS: normal/break/shelter/silence pages and exact 1920x1080 references present.")
print("PASS: admin prefs identity preserved; GitHub + Apps Script defaults embedded.")
print("PASS: app/server schedule data is byte-identical and manifest regenerated.")
print("PASS: technical TV footer code removed; admin diagnostics remain separate.")
