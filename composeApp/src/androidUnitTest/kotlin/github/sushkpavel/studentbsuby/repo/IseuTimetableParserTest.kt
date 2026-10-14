package github.sushkpavel.studentbsuby.repo

import github.sushkpavel.studentbsuby.util.exceptions.IncorrectResponseException
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class IseuTimetableParserTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(name)) { "missing fixture $name" }
            .bufferedReader().use { it.readText() }

    private val timetable = fixture("iseu_timetable.html")

    private val week = LocalDate(2026, 10, 12)

    private fun parse(html: String = timetable, group: String = "А61ИТ2", week: LocalDate = this.week) =
        IseuTimetableParser.parse(html, "user", group, week)

    @Test
    fun `lessons of the week`() {
        val week = parse()

        assertEquals(6, week.size)
        assertEquals(listOf(3, 5, 6, 5, 5, 0), week.map { it.size })
        assertTrue(week.flatten().all { it.owner == "user" })
        week.forEachIndexed { day, lessons ->
            assertTrue(lessons.all { it.dayOfWeek == day })
            assertEquals((1..lessons.size).toList(), lessons.map { it.number })
            assertEquals(lessons.sortedBy { it.starts }, lessons)
        }

        with(week[0][0]) {
            assertEquals("Физика", name)
            assertEquals("лек.", type)
            assertEquals("доц. Луцевич Александр Александрович", teacher)
            assertEquals("103, К2", place)
            assertEquals("08:30", starts)
            assertEquals("09:50", ends)
            assertEquals("2026-10-12", date)
        }
        assertEquals("практ. зан.", week[0][2].type)
        assertEquals("2026-10-16", week[4][0].date)
    }

    @Test
    fun `parallel groups of a lesson are merged`() {
        with(parse()[1][3]) {
            assertEquals("Иностранный язык", name)
            assertEquals("13:25", starts)
            assertEquals("410, К2 \\ 412, К2 \\ 205, К2 \\ 301, К2", place)
            assertEquals(4, teacher.split(" \\ ").size)
        }
    }

    @Test
    fun `parallel subgroups keep their names aligned with teachers and rooms`() {
        with(parse()[3][0]) {
            assertEquals(
                "Общая и неорганическая химия (2п/гр) \\ Основы алгоритмизации и программирования (1п/гр)",
                name
            )
            assertEquals("лаб.", type)
            assertEquals("пр. Борсток Елена Петровна \\ доц. Смирнова Татьяна Васильевна", teacher)
            assertEquals("109, К3 \\ 309, К2", place)
        }
    }

    @Test
    fun `lessons without type, teacher, room or discipline`() {
        val week = parse()
        with(week[1][1]) {
            assertEquals("Физическая культура", name)
            assertEquals("", type)
            assertEquals("", teacher)
            assertEquals("", place)
        }
        assertEquals("информационный час", week[2].last().name)
        assertEquals("Кураторский час", week[3].last().name)
    }

    @Test
    fun `week without lessons`() {
        val week = parse(fixture("iseu_timetable_empty_week.html"), week = LocalDate(2024, 9, 2))

        assertEquals(6, week.size)
        assertTrue(week.all { it.isEmpty() })
    }

    @Test
    fun `sunday lessons add the seventh day`() {
        val html = timetable.replace(
            "<td class=\"cell-date\"><span class='day'>Воскресенье</span> <span class='date'>18.10.2026</span></td><td class=\"cell-empty\" colspan=\"5\"></td>",
            "<td class=\"cell-date\"><span class='day'>Воскресенье</span> <span class='date'>18.10.2026</span></td><td class=\"cell-time\">9:00-10:20</td><td class=\"cell-subgroup\"></td><td class=\"cell-discipline\">лек. Экология</td><td class=\"cell-staff\"></td><td class=\"cell-auditory\">ауд. 1, К1</td>"
        )
        val week = parse(html)

        assertEquals(7, week.size)
        with(week[6].single()) {
            assertEquals("Экология", name)
            assertEquals("09:00", starts)
            assertEquals("2026-10-18", date)
        }
    }

    @Test
    fun `time written with dots and spaces`() {
        val week = parse(timetable.replace(">08:30-09:50<", ">8.30 - 9.50<"))

        assertEquals("08:30", week[0][0].starts)
        assertEquals("09:50", week[0][0].ends)
    }

    @Test
    fun `timetable of another group is rejected`() {
        assertFailsWith<IncorrectResponseException> { parse(group = "А61ИТ1") }
    }

    @Test
    fun `timetable of another week is rejected`() {
        assertFailsWith<IncorrectResponseException> { parse(week = LocalDate(2026, 10, 19)) }
    }

    @Test
    fun `page without the timetable caption is rejected`() {
        val html = timetable.replace(Regex("<caption>.*?</caption>", RegexOption.DOT_MATCHES_ALL), "")
        assertFailsWith<IncorrectResponseException> { parse(html) }
    }

    @Test
    fun `page without the timetable is rejected`() {
        assertFailsWith<IncorrectResponseException> { parse(fixture("iseu_page.html")) }
    }

    @Test
    fun `changed day markup is not taken for a free week`() {
        assertFailsWith<IncorrectResponseException> { parse(timetable.replace("cell-date", "cell-day")) }
    }

    @Test
    fun `changed time markup is not taken for a free week`() {
        val html = timetable.replace(Regex(">\\d{2}:\\d{2}-\\d{2}:\\d{2}<"), ">пара<")
        assertFailsWith<IncorrectResponseException> { parse(html) }
    }
}
