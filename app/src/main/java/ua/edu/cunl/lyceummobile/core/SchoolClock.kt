package ua.edu.cunl.lyceummobile.core

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

object SchoolClock {
    val kyivZone: ZoneId = runCatching { ZoneId.of("Europe/Kyiv") }
        .getOrElse { runCatching { ZoneId.of("Europe/Kiev") }.getOrDefault(ZoneId.systemDefault()) }

    private val uk = Locale.forLanguageTag("uk-UA")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", uk)

    fun now(): ZonedDateTime = ZonedDateTime.now(kyivZone)

    fun zoned(epochMillis: Long): ZonedDateTime =
        Instant.ofEpochMilli(epochMillis).atZone(kyivZone)

    fun isoDate(epochMillis: Long): String = zoned(epochMillis).toLocalDate().toString()

    fun parseIsoDate(value: String): LocalDate? =
        runCatching { LocalDate.parse(value) }.getOrNull()

    fun dayKey(epochMillis: Long): String = zoned(epochMillis).dayOfWeek.name

    fun minuteOfDay(epochMillis: Long): Int {
        val z = zoned(epochMillis)
        return z.hour * 60 + z.minute
    }

    fun isDailySilence(epochMillis: Long): Boolean {
        val z = zoned(epochMillis)
        return z.hour == 9 && z.minute == 0
    }

    fun secondsRemainingInSilenceMinute(epochMillis: Long): Int {
        if (!isDailySilence(epochMillis)) return 60
        return 60 - zoned(epochMillis).second
    }

    fun displayTime(epochMillis: Long): String = zoned(epochMillis).format(timeFormatter)

    fun displayDate(epochMillis: Long): String {
        val z = zoned(epochMillis)
        val weekday = z.dayOfWeek.getDisplayName(TextStyle.FULL, uk)
        val month = z.month.getDisplayName(TextStyle.FULL, uk)
        return "$weekday, ${z.dayOfMonth} $month"
    }

    fun epochMillis(localDate: LocalDate, hour: Int = 12, minute: Int = 0): Long =
        localDate.atTime(hour, minute).atZone(kyivZone).toInstant().toEpochMilli()
}

object WeekCycle {
    const val DEFAULT_REFERENCE = "2026-08-31"

    fun type(epochMillis: Long, numeratorMonday: String): WeekType {
        val reference = SchoolClock.parseIsoDate(numeratorMonday)
            ?: SchoolClock.parseIsoDate(DEFAULT_REFERENCE)
            ?: return WeekType.NUMERATOR
        val target = SchoolClock.zoned(epochMillis).toLocalDate()
        val refMonday = reference.with(DayOfWeek.MONDAY)
        val targetMonday = target.with(DayOfWeek.MONDAY)
        val weeks = ChronoUnit.WEEKS.between(refMonday, targetMonday)
        return if (Math.floorMod(weeks, 2L) == 0L) {
            WeekType.NUMERATOR
        } else {
            WeekType.DENOMINATOR
        }
    }
}
