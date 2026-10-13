package github.sushkpavel.studentbsuby.ui.screens.drawer.timetable

import github.sushkpavel.studentbsuby.data.models.Lesson
import github.sushkpavel.studentbsuby.ui.screens.drawer.timetable.LessonState.INCOMING
import github.sushkpavel.studentbsuby.ui.screens.drawer.timetable.LessonState.PASSED
import github.sushkpavel.studentbsuby.ui.screens.drawer.timetable.LessonState.RUNNING
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class LessonStatesTest {

    private fun lesson(day: Int, starts: String, ends: String, date: String = "") = Lesson(
        owner = "user", dayOfWeek = day, number = 1, name = "$day $starts",
        place = "", type = "", teacher = "", starts = starts, ends = ends, date = date
    )

    private val tuesday = LocalDate(2026, 10, 13)

    private fun states(week: List<List<Lesson>>, today: LocalDate = tuesday, time: String) =
        week.withStates(today, time).map { day -> day.map { it.second } }

    @Test
    fun `weekly timetable is compared by weekdays`() {
        val week = listOf(
            listOf(lesson(0, "08:30", "09:50")),
            listOf(lesson(1, "08:30", "09:50"), lesson(1, "10:05", "11:25"), lesson(1, "11:35", "12:55")),
            listOf(lesson(2, "08:30", "09:50")),
        )

        assertEquals(
            listOf(listOf(PASSED), listOf(PASSED, RUNNING, INCOMING), listOf(INCOMING)),
            states(week, time = "10:00")
        )
    }

    @Test
    fun `only the first unfinished lesson of today is running`() {
        val today = listOf(
            listOf(lesson(1, "08:30", "09:50"), lesson(1, "10:05", "11:25")),
        )
        assertEquals(listOf(listOf(RUNNING, INCOMING)), states(today, time = "07:00"))
        assertEquals(listOf(listOf(PASSED, PASSED)), states(today, time = "11:25"))
    }

    @Test
    fun `dated timetable is compared by dates`() {
        val week = listOf(
            listOf(lesson(0, "08:30", "09:50", "2026-10-12")),
            listOf(lesson(1, "08:30", "09:50", "2026-10-13"), lesson(1, "10:05", "11:25", "2026-10-13")),
            listOf(lesson(2, "08:30", "09:50", "2026-10-14")),
        )
        assertEquals(
            listOf(listOf(PASSED), listOf(PASSED, RUNNING), listOf(INCOMING)),
            states(week, time = "10:00")
        )
    }

    @Test
    fun `next week shown on sunday is incoming`() {
        val sunday = LocalDate(2026, 10, 18)
        val nextWeek = listOf(
            listOf(lesson(0, "08:30", "09:50", "2026-10-19")),
            listOf(lesson(1, "08:30", "09:50", "2026-10-20")),
        )
        assertEquals(listOf(listOf(INCOMING), listOf(INCOMING)), states(nextWeek, sunday, "20:00"))
    }

    @Test
    fun `weekly timetable has passed on sunday`() {
        val sunday = LocalDate(2026, 10, 18)
        val week = listOf(listOf(lesson(0, "08:30", "09:50")), listOf(lesson(5, "08:30", "09:50")))

        assertEquals(listOf(listOf(PASSED), listOf(PASSED)), states(week, sunday, "08:00"))
    }
}
