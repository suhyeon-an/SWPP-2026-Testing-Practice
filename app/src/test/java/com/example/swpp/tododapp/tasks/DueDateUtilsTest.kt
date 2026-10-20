package com.example.swpp.tododapp.tasks

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DueDateUtilsTest {
    @Test
    fun dueDateLabel_completedWithAnyDueDate_returnsClear() {
        // Given
        val today = LocalDate.parse("2026-10-19")
        val dates = listOf(null, "2026-10-18", "2026-10-19", "2026-10-20")
        // When
        val actual = dates.map { dueDateLabel(it?.let(LocalDate::parse), today, true) }
        // Then
        assertEquals(listOf("CLEAR", "CLEAR", "CLEAR", "CLEAR"), actual)
    }

    @Test
    fun dueDateLabel_activeWithoutDueDate_returnsEmpty() {
        // Given
        val today = LocalDate.parse("2026-10-19")
        // When
        val actual = dueDateLabel(null, today, false)
        // Then
        assertEquals("", actual)
    }

    @Test
    fun dueDateLabel_dueToday_returnsDDay() {
        // Given
        val today = LocalDate.parse("2026-10-19")
        // When
        val actual = dueDateLabel(LocalDate.parse("2026-10-19"), today, false)
        // Then
        assertEquals("D-Day", actual)
    }

    @Test
    fun dueDateLabel_futureAcrossMonths_returnsDMinus62() {
        // Given
        val today = LocalDate.parse("2026-10-19")
        val dueDate = LocalDate.parse("2026-12-20")
        // When
        val actual = dueDateLabel(dueDate, today, false)
        // Then
        assertEquals("D-62", actual)
    }

    @Test
    fun dueDateLabel_overdueAcrossMonths_returnsDPlus62() {
        // Given
        val today = LocalDate.parse("2026-12-20")
        val dueDate = LocalDate.parse("2026-10-19")
        // When
        val actual = dueDateLabel(dueDate, today, false)
        // Then
        assertEquals("D+62", actual)
    }

    @Test
    fun dueDateLabel_nullDueDateInEitherCompletionState_returnsValidLabels() {
        // Given
        val today = LocalDate.parse("2026-10-19")
        val states = listOf(false, true)
        // When - An unexpected exception also fails this test.
        val actual = states.map { dueDateLabel(null, today, it) }
        // Then
        assertEquals(listOf("", "CLEAR"), actual)
    }

    @Test
    fun dueDateLabel_yesterdayTodayTomorrow_returnsAdjacentDayLabels() {
        // Given
        val today = LocalDate.parse("2026-10-19")
        val dates = listOf("2026-10-18", "2026-10-19", "2026-10-20")
        // When
        val actual = dates.map { dueDateLabel(LocalDate.parse(it), today, false) }
        // Then
        assertEquals(listOf("D+1", "D-Day", "D-1"), actual)
    }

    @Test
    fun dueDateLabel_completionAcrossDateCategories_overridesDateLabel() {
        // Given
        val today = LocalDate.parse("2026-10-19")
        val dates = listOf(null, "2026-10-18", "2026-10-19", "2026-10-20")
        // When
        val actual = dates.map {
            val dueDate = it?.let(LocalDate::parse)
            dueDateLabel(dueDate, today, false) to dueDateLabel(dueDate, today, true)
        }
        // Then
        assertEquals(
            listOf("" to "CLEAR", "D+1" to "CLEAR", "D-Day" to "CLEAR", "D-1" to "CLEAR"),
            actual
        )
    }

    @Test
    fun dueDateLabel_monthRollover_countsAllDaysInBothDirections() {
        // Given
        val dates = listOf(
            "2026-01-30" to "2026-01-31",
            "2026-01-30" to "2026-02-01",
            "2026-01-30" to "2026-02-02",
            "2026-01-31" to "2026-01-30",
            "2026-02-01" to "2026-01-30",
            "2026-02-02" to "2026-01-30"
        )
        // When
        val actual = activeLabels(dates)
        // Then
        assertEquals(listOf("D-1", "D-2", "D-3", "D+1", "D+2", "D+3"), actual)
    }

    @Test
    fun dueDateLabel_fullMonthAndAdjacentDays_doesNotResetDayCount() {
        // Given
        val dates = listOf(
            "2026-01-01" to "2026-01-31",
            "2026-01-01" to "2026-02-01",
            "2026-01-01" to "2026-02-02",
            "2026-01-31" to "2026-01-01",
            "2026-02-01" to "2026-01-01",
            "2026-02-02" to "2026-01-01"
        )
        // When
        val actual = activeLabels(dates)
        // Then
        assertEquals(listOf("D-30", "D-31", "D-32", "D+30", "D+31", "D+32"), actual)
    }

    @Test
    fun dueDateLabel_yearRollover_countsAllDaysInBothDirections() {
        // Given
        val dates = listOf(
            "2026-12-30" to "2026-12-31",
            "2026-12-30" to "2027-01-01",
            "2026-12-30" to "2027-01-02",
            "2026-12-31" to "2026-12-30",
            "2027-01-01" to "2026-12-30",
            "2027-01-02" to "2026-12-30"
        )
        // When
        val actual = activeLabels(dates)
        // Then
        assertEquals(listOf("D-1", "D-2", "D-3", "D+1", "D+2", "D+3"), actual)
    }

    @Test
    fun dueDateLabel_leapDayAndAdjacentDays_countsAllDaysInBothDirections() {
        // Given
        val dates = listOf(
            "2024-02-27" to "2024-02-28",
            "2024-02-27" to "2024-02-29",
            "2024-02-27" to "2024-03-01",
            "2024-02-28" to "2024-02-27",
            "2024-02-29" to "2024-02-27",
            "2024-03-01" to "2024-02-27"
        )
        // When
        val actual = activeLabels(dates)
        // Then
        assertEquals(listOf("D-1", "D-2", "D-3", "D+1", "D+2", "D+3"), actual)
    }

    @Test
    fun dueDateLabel_nonLeapFebruaryRollover_countsAllDaysInBothDirections() {
        // Given
        val dates = listOf(
            "2026-02-27" to "2026-02-28",
            "2026-02-27" to "2026-03-01",
            "2026-02-27" to "2026-03-02",
            "2026-02-28" to "2026-02-27",
            "2026-03-01" to "2026-02-27",
            "2026-03-02" to "2026-02-27"
        )
        // When
        val actual = activeLabels(dates)
        // Then
        assertEquals(listOf("D-1", "D-2", "D-3", "D+1", "D+2", "D+3"), actual)
    }

    private fun activeLabels(dates: List<Pair<String, String>>): List<String> =
        dates.map { (today, dueDate) ->
            dueDateLabel(LocalDate.parse(dueDate), LocalDate.parse(today), false)
        }
}
