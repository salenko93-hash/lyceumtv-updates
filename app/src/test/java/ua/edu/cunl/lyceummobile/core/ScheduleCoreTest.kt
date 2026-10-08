package ua.edu.cunl.lyceummobile.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ScheduleCoreTest {
    private fun at(iso: String): Long =
        SchoolClock.epochMillis(LocalDate.parse(iso))

    @Test
    fun weekRotationMatchesIOS() {
        assertEquals(
            WeekType.NUMERATOR,
            WeekCycle.type(at("2026-08-31"), "2026-08-31")
        )
        assertEquals(
            WeekType.DENOMINATOR,
            WeekCycle.type(at("2026-09-07"), "2026-08-31")
        )
        assertEquals(
            WeekType.NUMERATOR,
            WeekCycle.type(at("2026-09-14"), "2026-08-31")
        )
        assertEquals(
            WeekType.DENOMINATOR,
            WeekCycle.type(at("2026-08-24"), "2026-08-31")
        )
    }

    @Test
    fun bellEnDashAndBoundaries() {
        val bell = BellSlot(2, "08:55–09:40")
        assertEquals(8 * 60 + 55, bell.minuteBounds?.first)
        assertEquals(9 * 60 + 39, bell.minuteBounds?.last)
        assertNull(BellSlot(1, "invalid").minuteBounds)
    }

    @Test
    fun latestMatchingSubstitutionWins() {
        val schedule = ScheduleFile(
            weekType = "NUMERATOR",
            bellSchedule = listOf(BellSlot(1, "08:00–08:45")),
            days = mapOf(
                "MONDAY" to mapOf(
                    1 to mapOf(
                        "10-А" to listOf(
                            LessonEntry("Алгебра", "7", "A")
                        )
                    )
                )
            )
        )
        val replacement = Substitution(
            date = "2026-08-31",
            week = "NUMERATOR",
            day = "MONDAY",
            lesson = 1,
            className = "10-А",
            scope = "both",
            entries = listOf(LessonEntry("Історія", "2", "B"))
        )
        val cancel = Substitution(
            date = "2026-08-31",
            week = "ALL",
            day = "MONDAY",
            lesson = 1,
            className = "10-А",
            scope = "normal",
            entries = emptyList()
        )

        val normal = ScheduleEngine.lessons(
            schedule,
            at("2026-08-31"),
            "10-А",
            listOf(replacement, cancel),
            shelter = false,
            referenceMonday = "2026-08-31"
        )
        assertEquals(0, normal.first().entries.size)

        val shelter = ScheduleEngine.lessons(
            schedule,
            at("2026-08-31"),
            "10-А",
            listOf(replacement, cancel),
            shelter = true,
            referenceMonday = "2026-08-31"
        )
        assertEquals("Історія", shelter.first().entries.first().subject)
    }

    @Test
    fun districtAlertUnknownNeverMeansClear() {
        assertEquals(DistrictAlert.ACTIVE, DistrictAlert.fromCode("\"A\""))
        assertEquals(DistrictAlert.PARTIAL, DistrictAlert.fromCode("P"))
        assertEquals(DistrictAlert.NONE, DistrictAlert.fromCode("N"))
        assertNull(DistrictAlert.fromCode("?"))
        assertNull(DistrictAlert.fromCode("ERROR"))
        assertFalse(DistrictAlert.NONE.isAlarm)
    }
}
