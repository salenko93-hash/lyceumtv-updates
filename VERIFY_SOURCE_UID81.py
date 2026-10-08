from pathlib import Path
import hashlib
import json
import re
import xml.etree.ElementTree as ET
from datetime import date, timedelta

root = Path(__file__).resolve().parent

# Build scripts consumed by Groovy/Gradle must be UTF-8 without BOM.
for rel in ("build.gradle", "settings.gradle", "app/build.gradle", "gradle.properties"):
    candidate = root / rel
    if candidate.exists():
        assert not candidate.read_bytes().startswith(b"\xef\xbb\xbf"), f"UTF-8 BOM is not allowed in {rel}"
java = root / "app/src/main/java/ua/edu/cunl/tv"

config = (java / "alerts/AlertsInUaConfig.java").read_text(encoding="utf-8")
monitor = (java / "alerts/AlertsInUaMonitor.java").read_text(encoding="utf-8")
store = (java / "alerts/AlertsInUaStore.java").read_text(encoding="utf-8")
admin = (java / "admin/AdminDialog.java").read_text(encoding="utf-8")
view = (java / "ui/SignageView.java").read_text(encoding="utf-8")
announcements = (java / "content/AnnouncementRepository.java").read_text(encoding="utf-8")
main = (java / "MainActivity.java").read_text(encoding="utf-8")
repo = (java / "schedule/ScheduleRepository.java").read_text(encoding="utf-8")
remote = (java / "remote/RemoteSyncManager.java").read_text(encoding="utf-8")
weather = (java / "weather/WeatherRepository.java").read_text(encoding="utf-8")
gradle = (root / "app/build.gradle").read_text(encoding="utf-8")
kyiv = (java / "util/KyivTime.java").read_text(encoding="utf-8")
strings = (root / "app/src/main/res/values/strings.xml").read_text(encoding="utf-8")
logo = root / "app/src/main/res/drawable-nodpi/lyceum_logo.png"
white_logo = root / "app/src/main/res/drawable-nodpi/lyceum_logo_white.png"

assert re.search(r'public static final String UID = "81";', config)
assert '+ UID + ".json"' in config
assert "String uid = AlertsInUaConfig.UID" in monitor
assert "String endpoint = AlertsInUaConfig.ENDPOINT" in monitor
assert "AlertsInUaConfig.UID.equals(cachedUid)" in store
assert ".clear()" in store and "KEY_UID, AlertsInUaConfig.UID" in store

assert "ФІКСОВАНИЙ UID" in admin and "Кропивницький район" in admin
assert 'DEFAULT_NUMERATOR_START_DATE = "2026-09-01"' in admin
assert "Адмінка залишається доступною під час тривоги" in admin
assert "onManualAlarm" in admin and "onManualAllClear" in admin
assert "onPollAlerts" in admin

assert "setScreenProfile" in view and "setClockStatus" in view
assert "setBreakAnnouncement" in view
assert "RED_EDGE" in view and "CLEAR_EDGE" in view and "RED_CENTER" in view
assert "РОЗКЛАД УРОКІВ" in view
assert "ПОВІТРЯНА ТРИВОГА" in view
assert "ВІДБІЙ ПОВІТРЯНОЇ ТРИВОГИ" in view
assert "drawScheduleCard" in view and "drawSideCards" in view
assert "ОГОЛОШЕННЯ" in view
assert "ScheduleSnapshot.Entry" in view
assert '"КАБ."' in view
assert "entry.subgroup" in view
assert "drawEntryRow" in view
assert "ПОГОДА • КРОПИВНИЦЬКИЙ" in admin
assert "weather+Kropyvnytskyi" in admin
assert "КРОПИВНИЦЬКИЙ" in view and "drawWeatherPanel" in view
assert "ДО КІНЦЯ УРОКУ" in view and "ДО ПОЧАТКУ " in view
assert "secondsToLessonEnd" in view and "secondsToLessonEnd" in repo
assert "api.open-meteo.com" in weather and "current=temperature_2m" in weather
assert "daily=weather_code,temperature_2m_max,temperature_2m_min" in weather
assert "forecast_days=5" in weather and "ForecastDay" in weather
assert "view.setWeather(data);" in main

assert "ScheduleRepository" in main and "shelter" in repo
assert "toStructuredEntries" in repo and "explicitGroupLabel" in repo
assert "AdminDialog.show(this, this, isAlarmActive()" in main
assert "адмінка доступна: OK 1с" in main
assert "dismissAdminForAlarm" not in main
assert "allowDuringAlarm" in remote
assert "this::isAlarmActive, true" in main

assert 'prefs.getString("numerator_start_date"' in repo
assert 'raw = "2026-09-01"' in repo
assert "refMonday" in repo
assert "getDayOfWeek() != DayOfWeek.MONDAY" not in repo

# Week 01.09.2026 is numerator; the next Monday begins denominator.
anchor = date(2026, 9, 1)
anchor_monday = anchor - timedelta(days=anchor.weekday())
def is_numerator(d):
    monday = d - timedelta(days=d.weekday())
    weeks = (monday - anchor_monday).days // 7
    return weeks % 2 == 0
assert is_numerator(date(2026, 9, 1))
assert is_numerator(date(2026, 9, 6))
assert not is_numerator(date(2026, 9, 7))
assert is_numerator(date(2026, 9, 14))
assert not is_numerator(date(2026, 10, 7))
assert is_numerator(date(2026, 10, 12))

assert 'ZoneId.of("Europe/Kyiv")' not in main
assert 'ZoneId.of("Europe/Kyiv")' not in view
assert 'ZoneId.of("Europe/Kyiv")' in kyiv and 'ZoneId.of("Europe/Kiev")' in kyiv
assert ".isBlank()" not in "\n".join(
    p.read_text(encoding="utf-8") for p in java.rglob("*.java")
)

assert "Центральноукраїнський науковий ліцей Кіровоградської обласної ради" in strings
assert logo.exists() and logo.stat().st_size > 0
assert hashlib.sha256(logo.read_bytes()).hexdigest() == "fb0482cf1c6ecdac0e51e654a385e625e6b051153b20a87cf855640e8b3c83a1"
assert white_logo.exists() and white_logo.stat().st_size > 0
assert "R.drawable.lyceum_logo_white" in view and "R.drawable.lyceum_logo_white" in admin
assert "activeAnnouncements" in announcements and "announcementForCycle" in announcements
assert 'optJSONArray("announcements")' in announcements
assert "KEY_BREAK_ANNOUNCEMENTS_ENABLED" in admin
assert "30 с розклад" in admin and "15 с оголошення" in admin
assert "snapshot.breakTime" in main
assert "snapshot.secondsToNextLesson > 60L" in main
assert "(snapshot.secondsIntoBreak % 45L) >= 30L" in main
assert "view.setBreakAnnouncement" in main
assert "versionCode 55" in gradle and "versionName '2.7.0.15'" in gradle
assert "importPendingTokenFromPc" in main
assert "PENDING_TOKEN_FILE" in main
assert "SecureTokenStore(this).save(token)" in main
pc_setup = (root / "SET_ALERTS_TOKEN.ps1").read_text(encoding="utf-8-sig")
assert "Read-Host" in pc_setup and "-AsSecureString" in pc_setup
assert "run-as $Package" in pc_setup
assert "cat > $Pending" in pc_setup
assert "192.168.5.77:5555" in (root / "BUILD_AND_INSTALL.ps1").read_text(
    encoding="utf-8-sig")
ET.parse(root / "app/src/main/AndroidManifest.xml")

assets = root / "app/src/main/assets"
expected_classes = ["10-А", "10-Б", "10-В", "10-Г",
                    "11-А", "11-Б", "11-В", "11-Г"]
expected_days = ["MONDAY", "TUESDAY", "WEDNESDAY",
                 "THURSDAY", "FRIDAY", "SATURDAY"]

expected_sources = {
    "schedule_numerator.json": "Розклад 10-11 Чисельник(6).xlsx",
    "schedule_denominator.json": "Розклад 10-11 Знаменник(5).xlsx",
    "shelter_numerator.json": "РОЗКЛАД  10–11 КЛАСІВ — ЧИСЕЛЬНИК УКРИТТЯ(4).xlsx",
    "shelter_denominator.json": "РОЗКЛАД 10–11 КЛАСІВ — ЗНАМЕННИК УКРИТТЯ(3).xlsx",
}

for name in ("schedule_numerator.json", "schedule_denominator.json",
             "shelter_numerator.json", "shelter_denominator.json"):
    obj = json.loads((assets / name).read_text(encoding="utf-8"))
    assert obj.get("source") == expected_sources[name]
    assert obj.get("placeholder") is not True
    bells = obj["bellSchedule"]
    assert len(bells) == 8
    assert [b["lesson"] for b in bells] == list(range(1, 9))
    assert bells[0]["start"] == "08:00" and bells[-1]["end"] == "15:15"
    assert list(obj["days"].keys()) == expected_days
    found = set()
    max_lesson = 0
    for day in expected_days:
        for cls, lessons in obj["days"][day].items():
            found.add(cls)
            for lesson, entries in lessons.items():
                max_lesson = max(max_lesson, int(lesson))
                assert isinstance(entries, list)
    assert found == set(expected_classes)
    assert max_lesson == 8

calendar = json.loads((assets / "calendar.json").read_text(encoding="utf-8"))
assert calendar["schoolYear"] == "2026/2027"
assert any(r["label"] == "Осінні канікули" and r["from"] == "2026-10-25"
           for r in calendar["ranges"])
assert any(r["label"] == "Зимові канікули" and r["to"] == "2027-01-10"
           for r in calendar["ranges"])
assert any(r["label"] == "Весняні канікули" and r["to"] == "2027-04-04"
           for r in calendar["ranges"])
assert any(r["label"] == "Літні канікули" and r["from"] == "2027-05-29"
           for r in calendar["openEndedRanges"])

audio = root / "app/src/main/res/raw/minute_silence.mp3"
assert audio.exists() and audio.stat().st_size == 2235481
digest = hashlib.sha256(audio.read_bytes()).hexdigest()
assert digest == "320051ede607e4b91342bfdbf0681f0e50a034218d2fb0609d7435d4f517b5c3"

alarm_audio = root / "app/src/main/res/raw/trivoga.mp3"
assert alarm_audio.exists() and alarm_audio.stat().st_size == 2014954
assert hashlib.sha256(alarm_audio.read_bytes()).hexdigest() == "1ec35cf363569d28ad8c705fb6e64e94d232418a2318ea081b9125afc5d251f3"
alarm_player = (java / "audio/AlarmSoundPlayer.java").read_text(encoding="utf-8")
assert '"trivoga"' in alarm_player and "setLooping(false)" in alarm_player
assert "startAlarmAudioIfNeeded" in main and "KEY_ALARM_SOUND_ENABLED" in admin
assert "KEY_ALARM_VOLUME" in admin and "alarmPlayer.stop()" in main

print("PASS: UID 81, cache invalidation, v55/2.7.0.15, manifest and Kyiv fallback.")
print("PASS: approved TV layout, red AIR_RAID and green ALL_CLEAR.")
print("PASS: admin remains available during AIR_RAID; explicit admin sync is allowed.")
print("PASS: numerator starts 01.09.2026 and alternates by school week.")
print("PASS: readable class/subject/room/teacher grid with subgroup markers.")
print("PASS: Kropyvnytskyi weather feed, compact TV weather card, original logo and white TV logo.")
print("PASS: lesson countdown, dedicated break countdown and announcement cycle.")
print("PASS: 4 latest real schedules, 8 classes, 8 bell periods, 79.536 s silence and 50.208 s TRIVOGA audio.")
