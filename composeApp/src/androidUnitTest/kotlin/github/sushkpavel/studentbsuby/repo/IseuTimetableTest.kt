package github.sushkpavel.studentbsuby.repo

import github.sushkpavel.studentbsuby.api.IseuTimetableApiImpl
import github.sushkpavel.studentbsuby.data.models.User
import github.sushkpavel.studentbsuby.util.exceptions.IncorrectResponseException
import github.sushkpavel.studentbsuby.util.exceptions.IseuGroupNotFoundException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IseuTimetableTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(name)) { "missing fixture $name" }
            .bufferedReader().use { it.readText() }

    private val institute = "Учреждение образования «Международный государственный " +
            "экологический институт имени А.Д.Сахарова» Белорусского государственного университета"

    private fun student(info: String) = checkNotNull(
        IseuStudent.from(User(username = "user", faculty = institute, info = info))
    )

    private val firstYear = student(
        "1 курс, группа А61ИТ2, форма обучения дневная, " +
                "специальность: информационные системы и технологии ФМОС"
    )

    @Test
    fun `student of the institute is recognized by the profile`() {
        assertEquals(
            IseuStudent(
                group = "А61ИТ2",
                course = 1,
                studyForm = "дневная",
                abbreviations = setOf("ФМОС")
            ),
            firstYear
        )
    }

    @Test
    fun `students of other faculties are not students of the institute`() {
        val user = User(
            username = "user",
            faculty = "Факультет прикладной математики и информатики",
            info = "1 курс, группа 5, форма обучения дневная"
        )
        assertNull(IseuStudent.from(user))
    }

    @Test
    fun `sunday shows the next week`() {
        assertEquals(LocalDate(2026, 10, 12), iseuWeekStart(LocalDate(2026, 10, 12)))
        assertEquals(LocalDate(2026, 10, 12), iseuWeekStart(LocalDate(2026, 10, 17)))
        assertEquals(LocalDate(2026, 10, 19), iseuWeekStart(LocalDate(2026, 10, 18)))
    }

    @Test
    fun `week timetable is parsed`() {
        val week = IseuTimetableParser.parse(fixture("iseu_timetable.html"), "user", "А61ИТ2")

        assertEquals(6, week.size)
        assertTrue(week[5].isEmpty())

        val monday = week[0]
        assertEquals(
            listOf("Физика", "Математический анализ", "Современная политэкономия"),
            monday.map { it.name }
        )
        with(monday[0]) {
            assertEquals("user", owner)
            assertEquals(0, dayOfWeek)
            assertEquals(1, number)
            assertEquals("лек.", type)
            assertEquals("доц. Луцевич Александр Александрович", teacher)
            assertEquals("103, К2", place)
            assertEquals("08:30", starts)
            assertEquals("09:50", ends)
            assertEquals("2026-10-12", date)
        }
        assertEquals("практ. зан.", monday[2].type)

        // four foreign language groups at the same time
        val tuesday = week[1]
        assertEquals(5, tuesday.size)
        with(tuesday[3]) {
            assertEquals(4, number)
            assertEquals("Иностранный язык", name)
            assertEquals("13:25", starts)
            assertEquals("410, К2 \\ 412, К2 \\ 205, К2 \\ 301, К2", place)
            assertEquals(4, teacher.split(" \\ ").size)
        }
        // no type, teacher and room
        with(tuesday[1]) {
            assertEquals("Физическая культура", name)
            assertEquals("", type)
            assertEquals("", teacher)
            assertEquals("", place)
        }

        assertEquals("информационный час", week[2].last().name)

        // subgroups at the same time
        val thursday = week[3]
        assertEquals(
            "Общая и неорганическая химия (2п/гр) \\ " +
                    "Основы алгоритмизации и программирования (1п/гр)",
            thursday[0].name
        )
        assertEquals("лаб.", thursday[0].type)
        assertEquals("109, К3 \\ 309, К2", thursday[0].place)
        // only a topic
        assertEquals("Кураторский час", thursday.last().name)
    }

    @Test
    fun `timetable of another group is rejected`() {
        assertFailsWith<IncorrectResponseException> {
            IseuTimetableParser.parse(fixture("iseu_timetable.html"), "user", "А61ИТ1")
        }
    }

    @Test
    fun `sunday lessons add the seventh day`() {
        val html = fixture("iseu_timetable.html").replace(
            "<td class=\"cell-date\"><span class='day'>Воскресенье</span> <span class='date'>18.10.2026</span></td><td class=\"cell-empty\" colspan=\"5\"></td>",
            "<td class=\"cell-date\"><span class='day'>Воскресенье</span> <span class='date'>18.10.2026</span></td><td class=\"cell-time\">09:00-10:20</td><td class=\"cell-subgroup\"></td><td class=\"cell-discipline\">лек. Экология</td><td class=\"cell-staff\"></td><td class=\"cell-auditory\">ауд. 1, К1</td>"
        )
        val week = IseuTimetableParser.parse(html, "user", "А61ИТ2")

        assertEquals(7, week.size)
        assertEquals(listOf("Экология"), week[6].map { it.name })
        assertEquals("2026-10-18", week[6][0].date)
    }

    /** Postbacks made by the loader; the page request is an empty map. */
    private class Site(private val respond: (Map<String, String>) -> String) {
        val requests = mutableListOf<Map<String, String>>()

        val api = IseuTimetableApiImpl(HttpClient(MockEngine { request ->
            val form = (request.body as? FormDataContent)?.formData?.let { params ->
                params.names().associateWith { params[it].orEmpty() }
            }.orEmpty()
            requests += form
            respond(
                respond(form),
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "text/html; charset=utf-8")
            )
        }))
    }

    private fun site(timetableGroup: String = "А61ИТ2") = Site { form ->
        when {
            form.isEmpty() -> fixture("iseu_page.html")
            "ShowTT" in form -> fixture("iseu_timetable.html").replace("А61ИТ2", timetableGroup)
            form["__EVENTTARGET"] == "ddlFac" -> fixture("iseu_page_faculty_4.html")
            form["__EVENTTARGET"] == "ddlCourse" -> fixture("iseu_page_course_2.html")
            else -> error("unexpected postback $form")
        }
    }

    @Test
    fun `group of the selected lists costs the page and one postback`() = runBlocking {
        val site = site()
        val week = IseuTimetableLoader(site.api).load("user", firstYear, LocalDate(2026, 10, 13))

        assertEquals(2, site.requests.size)
        val show = site.requests[1]
        assertEquals("Показать", show["ShowTT"])
        assertEquals("2", show["ddlFac"])
        assertEquals("2", show["ddlDep"])
        assertEquals("1", show["ddlCourse"])
        assertEquals("370", show["ddlGroup"])
        assertEquals("12.10.2026 0:00:00", show["ddlWeek"])
        assertTrue(show["__VIEWSTATE"].orEmpty().isNotEmpty())
        assertTrue(show["__EVENTVALIDATION"].orEmpty().isNotEmpty())

        assertEquals(3, week[0].size)
    }

    @Test
    fun `next week is requested on sunday`() = runBlocking {
        val site = site()
        IseuTimetableLoader(site.api).load("user", firstYear, LocalDate(2026, 10, 18))

        assertEquals("19.10.2026 0:00:00", site.requests.last()["ddlWeek"])
    }

    @Test
    fun `faculty is switched by the abbreviation in the profile`() = runBlocking {
        val site = site(timetableGroup = "А61МД2")
        val student = student(
            "1 курс, группа А61МД2, форма обучения дневная, специальность: лечебное дело ФЭМ"
        )
        IseuTimetableLoader(site.api).load("user", student, LocalDate(2026, 10, 13))

        assertEquals(3, site.requests.size)
        assertEquals("4", site.requests[1]["ddlFac"])
        assertEquals("375", site.requests[2]["ddlGroup"])
        assertEquals("4", site.requests[2]["ddlFac"])
    }

    @Test
    fun `course is switched by the profile`() = runBlocking {
        val site = site(timetableGroup = "А51ИТ2")
        val student = student(
            "2 курс, группа a51ит2, форма обучения дневная, " +
                    "специальность: информационные системы и технологии ФМОС"
        )
        IseuTimetableLoader(site.api).load("user", student, LocalDate(2026, 10, 13))

        assertEquals(3, site.requests.size)
        assertEquals("ddlCourse", site.requests[1]["__EVENTTARGET"])
        assertEquals("2", site.requests[1]["ddlCourse"])
        assertEquals("332", site.requests[2]["ddlGroup"])
    }

    @Test
    fun `unknown group is reported without searching other faculties`() = runBlocking {
        val site = site()
        val student = student(
            "1 курс, группа А61ИТ9, форма обучения дневная, " +
                    "специальность: информационные системы и технологии ФМОС"
        )
        assertFailsWith<IseuGroupNotFoundException> {
            IseuTimetableLoader(site.api).load("user", student, LocalDate(2026, 10, 13))
        }
        assertEquals(1, site.requests.size)
    }
}
