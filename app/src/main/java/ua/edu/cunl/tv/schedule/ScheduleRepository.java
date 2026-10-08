package ua.edu.cunl.tv.schedule;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ScheduleRepository {
    private static final List<String> CLASS_ORDER = Arrays.asList(
            "10-А", "10-Б", "10-В", "10-Г", "11-А", "11-Б", "11-В", "11-Г"
    );

    private final Context context;

    public ScheduleRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public ScheduleSnapshot snapshot(ZonedDateTime now, boolean shelter, SharedPreferences prefs) {
        try {
            String day = dayKey(now.getDayOfWeek());
            if (day == null) {
                return ScheduleSnapshot.message("НЕДІЛЯ", "Занять за розкладом немає",
                        "NO_SCHEDULE", false, false);
            }

            String holiday = holidayLabel(now.toLocalDate());
            if (!holiday.isEmpty()) {
                return ScheduleSnapshot.message("КАНІКУЛИ", holiday, "HOLIDAY", true, false);
            }

            Week week = resolveWeek(now.toLocalDate(), prefs);
            String filename = (shelter ? "shelter_" : "schedule_")
                    + (week.numerator ? "numerator" : "denominator") + ".json";
            JSONObject root = readJsonPreferRemote(filename);
            JSONObject dayObj = root.getJSONObject("days").optJSONObject(day);
            if (dayObj == null) {
                return ScheduleSnapshot.message("РОЗКЛАД", "Немає даних на " + day,
                        "NO_SCHEDULE", false, false);
            }

            JSONArray bells = root.getJSONArray("bellSchedule");
            int lastActual = lastActualLesson(dayObj);
            if (lastActual <= 0) {
                return ScheduleSnapshot.message("РОЗКЛАД", "Занять сьогодні немає",
                        "NO_SCHEDULE", false, false);
            }

            Period period = resolvePeriod(now.toLocalTime(), bells, lastActual);
            String mode = week.numerator ? "ЧИСЕЛЬНИК" : "ЗНАМЕННИК";
            String scope = shelter ? " • УКРИТТЯ" : "";
            String subtitle = mode + scope;

            if (period.afterSchool) {
                return ScheduleSnapshot.message("ПІСЛЯ УРОКІВ", subtitle,
                        "AFTER_SCHOOL", false, true);
            }

            if (period.lesson <= 0) {
                return ScheduleSnapshot.message("ДО УРОКІВ", subtitle,
                        "BEFORE_SCHOOL", false, false);
            }

            JSONObject content = readOptionalJsonPreferRemote("content.json");
            List<ScheduleSnapshot.Row> rows = new ArrayList<>();
            for (String className : CLASS_ORDER) {
                JSONArray entries = entriesFor(dayObj, className, period.lesson);
                JSONArray replacement = matchingSubstitution(content, now.toLocalDate(), day,
                        mode, className, period.lesson, shelter);
                if (replacement != null) entries = replacement;

                rows.add(new ScheduleSnapshot.Row(className, toStructuredEntries(entries)));
            }

            String phaseText = period.inLesson ? "УРОК " + period.lesson
                    : "ПЕРЕРВА • ДАЛІ УРОК " + period.lesson;
            String title = shelter ? "РОЗКЛАД В УКРИТТІ" : "РОЗКЛАД";
            return new ScheduleSnapshot(title, subtitle, phaseText, period.lesson,
                    false, false, period.breakTime, period.secondsToLessonEnd,
                    period.secondsToNextLesson, period.secondsIntoBreak, rows);
        } catch (Exception e) {
            return ScheduleSnapshot.message("РОЗКЛАД", "Помилка читання: "
                    + e.getClass().getSimpleName(), "ERROR", false, false);
        }
    }

    private JSONArray entriesFor(JSONObject dayObj, String className, int lesson) {
        JSONObject c = dayObj.optJSONObject(className);
        if (c == null) return new JSONArray();
        JSONArray a = c.optJSONArray(String.valueOf(lesson));
        return a == null ? new JSONArray() : a;
    }

    private static List<ScheduleSnapshot.Entry> toStructuredEntries(JSONArray entries) {
        List<ScheduleSnapshot.Entry> out = new ArrayList<>();
        if (entries == null) return out;

        for (int i = 0; i < entries.length(); i++) {
            JSONObject e = entries.optJSONObject(i);
            if (e == null) continue;

            String subject = e.optString("subject", "").trim();
            String room = e.optString("room", "").trim();
            String teacher = e.optString("teacher", "").trim();

            String subgroup = explicitGroupLabel(subject, room, teacher);
            subject = stripLeadingGroupLabel(subject);
            room = stripLeadingGroupLabel(room);
            teacher = stripLeadingGroupLabel(teacher);

            if (subject.isEmpty() && room.isEmpty() && teacher.isEmpty()) continue;
            out.add(new ScheduleSnapshot.Entry(subgroup, subject, room, teacher));
        }
        return out;
    }

    private static String explicitGroupLabel(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value == null) continue;
            String s = value.trim();
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                    "(?iu)^(?:гр\\.?|група|підгрупа)\\s*([0-9]+)\\s*:?").matcher(s);
            if (m.find()) return "ГР. " + m.group(1);
        }
        return "";
    }

    private static String stripLeadingGroupLabel(String value) {
        if (value == null) return "";
        return value.trim().replaceFirst(
                "(?iu)^(?:гр\\.?|група|підгрупа)\\s*[0-9]+\\s*:?\\s*", "").trim();
    }

    private JSONArray matchingSubstitution(JSONObject content, LocalDate date, String day,
                                           String week, String className, int lesson,
                                           boolean shelter) {
        if (content == null) return null;
        JSONArray substitutions = content.optJSONArray("substitutions");
        if (substitutions == null) return null;
        String wantedScope = shelter ? "shelter" : "normal";
        for (int i = 0; i < substitutions.length(); i++) {
            JSONObject s = substitutions.optJSONObject(i);
            if (s == null || !s.optBoolean("active", false)) continue;
            if (!matches(s.optString("date", ""), date.toString())) continue;
            if (!matchesUpper(s.optString("day", ""), day)) continue;
            String w = s.optString("week", "all").trim().toUpperCase(Locale.ROOT);
            if (!(w.isEmpty() || "ALL".equals(w) || w.equals(week))) continue;
            if (!matches(s.optString("class", ""), className)) continue;
            int l = s.optInt("lesson", -1);
            if (l != lesson) continue;
            String scope = s.optString("scope", "both").trim().toLowerCase(Locale.ROOT);
            if (!(scope.equals("both") || scope.equals(wantedScope))) continue;
            JSONArray entries = s.optJSONArray("entries");
            return entries == null ? new JSONArray() : entries;
        }
        return null;
    }

    private static boolean matches(String criterion, String actual) {
        String c = criterion == null ? "" : criterion.trim();
        return c.isEmpty() || c.equalsIgnoreCase("all") || c.equals(actual);
    }

    private static boolean matchesUpper(String criterion, String actual) {
        String c = criterion == null ? "" : criterion.trim().toUpperCase(Locale.ROOT);
        return c.isEmpty() || c.equals("ALL") || c.equals(actual);
    }

    private Week resolveWeek(LocalDate date, SharedPreferences prefs) {
        // The 2026/2027 school year starts with numerator on 01.09.2026.
        // The supplied start date may be any weekday; alternating is calculated
        // by the Monday containing that date. Thus 01.09.2026 belongs to the
        // numerator week whose Monday is 31.08.2026.
        String raw = prefs.getString("numerator_start_date", "").trim();
        if (raw.isEmpty()) {
            raw = prefs.getString("reference_numerator_monday", "").trim();
        }
        if (raw.isEmpty()) raw = "2026-09-01";

        try {
            LocalDate refDate = LocalDate.parse(raw);
            LocalDate refMonday = refDate.minusDays(refDate.getDayOfWeek().getValue() - 1L);
            LocalDate monday = date.minusDays(date.getDayOfWeek().getValue() - 1L);
            long weeks = ChronoUnit.WEEKS.between(refMonday, monday);
            return new Week(Math.floorMod(weeks, 2) == 0, true);
        } catch (Exception e) {
            LocalDate refDate = LocalDate.of(2026, 9, 1);
            LocalDate refMonday = refDate.minusDays(refDate.getDayOfWeek().getValue() - 1L);
            LocalDate monday = date.minusDays(date.getDayOfWeek().getValue() - 1L);
            long weeks = ChronoUnit.WEEKS.between(refMonday, monday);
            return new Week(Math.floorMod(weeks, 2) == 0, true);
        }
    }

    private String holidayLabel(LocalDate date) {
        try {
            JSONObject cal = readJsonPreferRemote("calendar.json");
            JSONArray daysOff = cal.optJSONArray("daysOff");
            if (daysOff != null) {
                for (int i = 0; i < daysOff.length(); i++) {
                    JSONObject d = daysOff.optJSONObject(i);
                    if (d != null && date.toString().equals(d.optString("date"))) {
                        return d.optString("label", "Вихідний");
                    }
                }
            }
            JSONArray ranges = cal.optJSONArray("ranges");
            if (ranges != null) {
                for (int i = 0; i < ranges.length(); i++) {
                    JSONObject r = ranges.optJSONObject(i);
                    if (r == null) continue;
                    LocalDate from = LocalDate.parse(r.getString("from"));
                    LocalDate to = LocalDate.parse(r.getString("to"));
                    if (!date.isBefore(from) && !date.isAfter(to)) {
                        return r.optString("label", "Канікули");
                    }
                }
            }
            JSONArray open = cal.optJSONArray("openEndedRanges");
            if (open != null) {
                for (int i = 0; i < open.length(); i++) {
                    JSONObject r = open.optJSONObject(i);
                    if (r == null) continue;
                    LocalDate from = LocalDate.parse(r.getString("from"));
                    if (!date.isBefore(from)) return r.optString("label", "Канікули");
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    private static int lastActualLesson(JSONObject dayObj) {
        int max = 0;
        for (String className : CLASS_ORDER) {
            JSONObject c = dayObj.optJSONObject(className);
            if (c == null) continue;
            java.util.Iterator<String> it = c.keys();
            while (it.hasNext()) {
                try { max = Math.max(max, Integer.parseInt(it.next())); }
                catch (NumberFormatException ignored) {}
            }
        }
        return max;
    }

    private static Period resolvePeriod(LocalTime now, JSONArray bells, int lastActual) {
        Map<Integer, LocalTime[]> byLesson = new LinkedHashMap<>();
        for (int i = 0; i < bells.length(); i++) {
            JSONObject b = bells.optJSONObject(i);
            if (b == null) continue;
            int lesson = b.optInt("lesson", -1);
            if (lesson < 1 || lesson > lastActual) continue;
            try {
                byLesson.put(lesson, new LocalTime[]{
                        LocalTime.parse(b.getString("start")),
                        LocalTime.parse(b.getString("end"))
                });
            } catch (Exception ignored) {}
        }
        if (byLesson.isEmpty()) return new Period(0, false, false, false, 0L, 0L, 0L);

        int first = byLesson.keySet().iterator().next();
        LocalTime firstStart = byLesson.get(first)[0];
        if (now.isBefore(firstStart)) {
            long seconds = Math.max(0L, Duration.between(now, firstStart).getSeconds());
            return new Period(first, false, false, false, 0L, seconds, 0L);
        }

        int previous = 0;
        for (Map.Entry<Integer, LocalTime[]> e : byLesson.entrySet()) {
            int lesson = e.getKey();
            LocalTime start = e.getValue()[0];
            LocalTime end = e.getValue()[1];

            if (!now.isBefore(start) && !now.isAfter(end)) {
                long remaining = Math.max(0L, Duration.between(now, end).getSeconds());
                return new Period(lesson, true, false, false, remaining, 0L, 0L);
            }

            if (previous > 0) {
                LocalTime prevEnd = byLesson.get(previous)[1];
                if (now.isAfter(prevEnd) && now.isBefore(start)) {
                    long seconds = Math.max(0L, Duration.between(now, start).getSeconds());
                    long elapsed = Math.max(0L, Duration.between(prevEnd, now).getSeconds());
                    return new Period(lesson, false, false, true, 0L, seconds, elapsed);
                }
            }
            previous = lesson;
        }

        int last = 0;
        LocalTime lastEnd = null;
        for (Map.Entry<Integer, LocalTime[]> e : byLesson.entrySet()) {
            last = e.getKey();
            lastEnd = e.getValue()[1];
        }
        if (lastEnd != null && now.isAfter(lastEnd)) {
            return new Period(last, false, true, false, 0L, 0L, 0L);
        }
        return new Period(last, false, false, false, 0L, 0L, 0L);
    }

    private JSONObject readJsonPreferRemote(String filename) throws Exception {
        File remote = new File(new File(new File(context.getFilesDir(), "remote"), "current"), filename);
        if (remote.isFile()) {
            try (InputStream in = new FileInputStream(remote)) {
                return new JSONObject(readAll(in));
            } catch (Exception ignored) {
                // Corrupt remote data must not break the packaged fallback.
            }
        }
        try (InputStream in = context.getAssets().open(filename)) {
            return new JSONObject(readAll(in));
        }
    }

    private JSONObject readOptionalJsonPreferRemote(String filename) {
        try { return readJsonPreferRemote(filename); }
        catch (Exception e) { return null; }
    }

    private static String readAll(InputStream in) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) >= 0) if (n > 0) out.write(buffer, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static String dayKey(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "MONDAY";
            case TUESDAY -> "TUESDAY";
            case WEDNESDAY -> "WEDNESDAY";
            case THURSDAY -> "THURSDAY";
            case FRIDAY -> "FRIDAY";
            case SATURDAY -> "SATURDAY";
            default -> null;
        };
    }

    private record Week(boolean numerator, boolean referenceConfigured) {}
    private record Period(int lesson, boolean inLesson, boolean afterSchool,
                          boolean breakTime, long secondsToLessonEnd,
                          long secondsToNextLesson, long secondsIntoBreak) {}
}
