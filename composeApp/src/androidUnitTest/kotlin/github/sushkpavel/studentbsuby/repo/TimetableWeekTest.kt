package github.sushkpavel.studentbsuby.repo

import github.sushkpavel.studentbsuby.data.models.Lesson
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TimetableWeekTest {

    private fun lesson(day: Int, date: String = "") = Lesson(
        owner = "user", dayOfWeek = day, number = 1, name = "$day",
        place = "", type = "", teacher = "", starts = "", ends = "", date = date
    )

    @Test
    fun `week from monday to saturday is the current one`() {
        (12..17).forEach { day ->
            assertEquals(LocalDate(2026, 10, 12), shownWeekStart(LocalDate(2026, 10, day)))
        }
    }

    @Test
    fun `on sunday the next week is shown`() {
        assertEquals(LocalDate(2026, 10, 19), shownWeekStart(LocalDate(2026, 10, 18)))
    }

    @Test
    fun `weeks across month and year boundaries`() {
        assertEquals(LocalDate(2026, 9, 28), shownWeekStart(LocalDate(2026, 10, 1)))
        assertEquals(LocalDate(2026, 12, 28), shownWeekStart(LocalDate(2026, 12, 27)))
        assertEquals(LocalDate(2026, 12, 28), shownWeekStart(LocalDate(2027, 1, 2)))
        assertEquals(LocalDate(2027, 1, 4), shownWeekStart(LocalDate(2027, 1, 3)))
    }

    @Test
    fun `week of a lesson`() {
        assertEquals(LocalDate(2026, 10, 12), lesson(6, "2026-10-18").weekStart())
        assertNull(lesson(0).weekStart())
        assertNull(lesson(0, "18.10.2026").weekStart())
    }

    @Test
    fun `week has a sunday only when there are lessons on it`() {
        assertEquals(6, emptyMap<Int, List<Lesson>>().toWeek().size)
        assertEquals(6, mapOf(5 to listOf(lesson(5))).toWeek().size)

        val week = mapOf(0 to listOf(lesson(0)), 6 to listOf(lesson(6))).toWeek()
        assertEquals(7, week.size)
        assertEquals(listOf("6"), week[6].map { it.name })
    }
}
