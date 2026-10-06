from pathlib import Path
import json
import re
import zipfile
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parent
java = root / "app/src/main/java/ua/edu/cunl/tv"
config = (java / "alerts/AlertsInUaConfig.java").read_text(encoding="utf-8")
monitor = (java / "alerts/AlertsInUaMonitor.java").read_text(encoding="utf-8")
store = (java / "alerts/AlertsInUaStore.java").read_text(encoding="utf-8")
admin = (java / "admin/AdminDialog.java").read_text(encoding="utf-8")
gradle = (root / "app/build.gradle").read_text(encoding="utf-8")
assert re.search(r'public static final String UID = "81";', config)
assert '+ UID + ".json"' in config
assert "String uid = AlertsInUaConfig.UID" in monitor
assert "String endpoint = AlertsInUaConfig.ENDPOINT" in monitor
assert "AlertsInUaConfig.UID.equals(cachedUid)" in store
assert ".clear()" in store and "KEY_UID, AlertsInUaConfig.UID" in store
assert "ФІКСОВАНИЙ UID" in admin
assert "Кропивницький район" in admin
assert "versionCode 44" in gradle and 'versionName "2.7.0.4"' in gradle
assert '192.168.5.77:5555' in (root / "BUILD_AND_INSTALL.ps1").read_text(encoding="utf-8-sig")
assert 'active_air_raid_alerts/81.json' in (root / "TEST_ALERTS_IN_UA_API.ps1").read_text(encoding="utf-8-sig")
ET.parse(root / "app/src/main/AndroidManifest.xml")
for asset in ("schedule_numerator.json", "schedule_denominator.json",
              "shelter_numerator.json", "shelter_denominator.json"):
    schedule = json.loads((root / "app/src/main/assets" / asset).read_text(encoding="utf-8"))
    assert len(schedule["bellSchedule"]) == 8 and schedule["days"]
print("PASS: single UID 81 endpoint, old-cache invalidation, build/version/IP, 4 schedules, manifest.")
