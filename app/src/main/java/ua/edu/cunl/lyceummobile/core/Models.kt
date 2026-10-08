package ua.edu.cunl.lyceummobile.core

data class LessonEntry(
    val subject: String = "",
    val room: String = "",
    val teacher: String = ""
)

data class BellSlot(
    val lesson: Int,
    val time: String
) {
    val minuteBounds: IntRange?
        get() {
            val matches = Regex("""\b([0-2]\d):([0-5]\d)\b""")
                .findAll(time).toList()
            if (matches.size != 2) return null
            fun minute(match: MatchResult): Int {
                val h = match.groupValues[1].toIntOrNull() ?: return -1
                val m = match.groupValues[2].toIntOrNull() ?: return -1
                return h * 60 + m
            }
            val start = minute(matches[0])
            val end = minute(matches[1])
            if (start < 0 || end <= start || end > 1440) return null
            return start until end
        }
}

data class ScheduleFile(
    val weekType: String,
    val source: String? = null,
    val title: String? = null,
    val bellSchedule: List<BellSlot>,
    val days: Map<String, Map<Int, Map<String, List<LessonEntry>>>>
) {
    fun entries(day: String, lesson: Int, className: String): List<LessonEntry> =
        days[day]?.get(lesson)?.get(className).orEmpty()
}

data class Announcement(
    val active: Boolean = true,
    val title: String = "",
    val message: String = "",
    val priority: String = "NORMAL"
)

data class SchoolEvent(
    val active: Boolean = true,
    val title: String = "",
    val date: String = "",
    val time: String = "",
    val location: String = ""
)

data class Substitution(
    val date: String? = null,
    val week: String? = null,
    val day: String? = null,
    val lesson: Int = 0,
    val className: String = "",
    val scope: String? = null,
    val active: Boolean = true,
    val entries: List<LessonEntry> = emptyList()
)

data class ContentFeed(
    val announcements: List<Announcement> = emptyList(),
    val events: List<SchoolEvent> = emptyList(),
    val substitutions: List<Substitution> = emptyList()
)

data class SchoolCalendar(
    val daysOff: List<DayOff> = emptyList(),
    val ranges: List<Range> = emptyList()
) {
    data class DayOff(val date: String, val label: String? = null)
    data class Range(val from: String, val to: String, val label: String? = null)

    fun labelFor(isoDate: String): String? {
        daysOff.firstOrNull { it.date == isoDate }?.let {
            return it.label ?: "Навчальних занять немає"
        }
        ranges.firstOrNull { isoDate >= it.from && isoDate <= it.to }?.let {
            return it.label ?: "Канікули"
        }
        return null
    }
}

enum class WeekType(val label: String, val fileSuffix: String) {
    NUMERATOR("ЧИСЕЛЬНИК", "numerator"),
    DENOMINATOR("ЗНАМЕННИК", "denominator")
}

enum class DistrictAlert(val code: String, val label: String) {
    UNKNOWN("?", "Стан тривоги невідомий"),
    ACTIVE("A", "ПОВІТРЯНА ТРИВОГА"),
    PARTIAL("P", "ЧАСТКОВА ПОВІТРЯНА ТРИВОГА"),
    NONE("N", "Тривога не підтверджена як активна");

    val isAlarm: Boolean get() = this == ACTIVE || this == PARTIAL

    companion object {
        fun fromCode(raw: String): DistrictAlert? {
            val clean = raw.trim().trim('"').uppercase()
            return entries.firstOrNull { it.code == clean && it != UNKNOWN }
        }
    }
}

data class LessonRow(
    val bell: BellSlot,
    val entries: List<LessonEntry>
)

data class TeacherLessonLocation(
    val lesson: Int,
    val time: String,
    val className: String,
    val subject: String,
    val room: String
)

data class TeacherDayStatus(
    val teacher: String,
    val lessons: List<TeacherLessonLocation>,
    val current: List<TeacherLessonLocation>,
    val next: List<TeacherLessonLocation>
)

