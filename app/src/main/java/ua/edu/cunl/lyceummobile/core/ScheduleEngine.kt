package ua.edu.cunl.lyceummobile.core

object ScheduleEngine {
    val classNames = listOf(
        "10-А", "10-Б", "10-В", "10-Г",
        "11-А", "11-Б", "11-В", "11-Г"
    )

    fun currentBell(schedule: ScheduleFile, epochMillis: Long): BellSlot? {
        val current = SchoolClock.minuteOfDay(epochMillis)
        return schedule.bellSchedule.firstOrNull { bell ->
            bell.minuteBounds?.let { current in it } == true
        }
    }

    fun nextBell(schedule: ScheduleFile, epochMillis: Long): BellSlot? {
        val current = SchoolClock.minuteOfDay(epochMillis)
        return schedule.bellSchedule.firstOrNull { bell ->
            bell.minuteBounds?.first?.let { it > current } == true
        }
    }

    fun lessons(
        schedule: ScheduleFile,
        epochMillis: Long,
        className: String,
        substitutions: List<Substitution>,
        shelter: Boolean,
        referenceMonday: String
    ): List<LessonRow> {
        val day = SchoolClock.dayKey(epochMillis)
        val week = WeekCycle.type(epochMillis, referenceMonday)
        val iso = SchoolClock.isoDate(epochMillis)

        return schedule.bellSchedule.map { bell ->
            var entries = schedule.entries(day, bell.lesson, className)
            for (row in substitutions.asReversed()) {
                if (!row.active || row.lesson != bell.lesson || row.className != className) continue
                if (!row.date.isNullOrBlank() && row.date != iso) continue
                if (!row.day.isNullOrBlank() && row.day.uppercase() != day) continue

                val scope = (row.scope ?: "normal").lowercase()
                val wantedScope = if (shelter) "shelter" else "normal"
                if (scope != "both" && scope != wantedScope) continue

                val choice = (row.week ?: "all").uppercase()
                val matchesWeek = choice == "ALL" ||
                    (week == WeekType.NUMERATOR &&
                        (choice == "NUMERATOR" || choice == "ЧИСЕЛЬНИК")) ||
                    (week == WeekType.DENOMINATOR &&
                        (choice == "DENOMINATOR" || choice == "ЗНАМЕННИК"))
                if (!matchesWeek) continue

                entries = row.entries
                break
            }
            LessonRow(bell, entries)
        }
    }
}
