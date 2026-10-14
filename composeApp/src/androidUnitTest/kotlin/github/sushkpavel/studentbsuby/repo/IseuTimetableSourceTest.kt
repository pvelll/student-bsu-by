package github.sushkpavel.studentbsuby.repo

import github.sushkpavel.studentbsuby.api.IseuTimetableApiImpl
import github.sushkpavel.studentbsuby.data.models.User
import github.sushkpavel.studentbsuby.network.iseuClientConfig
import github.sushkpavel.studentbsuby.util.exceptions.FailResponseException
import github.sushkpavel.studentbsuby.util.exceptions.GroupNotFoundException
import github.sushkpavel.studentbsuby.util.exceptions.IncorrectResponseException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IseuTimetableSourceTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(name)) { "missing fixture $name" }
            .bufferedReader().use { it.readText() }

    private val institute = "Международный государственный экологический институт имени А.Д.Сахарова"

    private fun user(info: String) = User(username = "user", faculty = institute, info = info)

    private val firstYear = user(
        "1 курс, группа А61ИТ2, форма обучения дневная, " +
                "специальность: информационные системы и технологии ФМОС"
    )

    private val tuesday = LocalDate(2026, 10, 13)
    private val sunday = LocalDate(2026, 10, 18)

    private class Site(
        retries: Boolean = false,
        private val handler: suspend MockRequestHandleScope.(Map<String, String>) -> HttpResponseData,
    ) {
        val requests = mutableListOf<Map<String, String>>()

        val source = IseuTimetableSource(IseuTimetableApiImpl(HttpClient(MockEngine { request ->
            val form = (request.body as? FormDataContent)?.formData?.let { params ->
                params.names().associateWith { params[it].orEmpty() }
            }.orEmpty()
            requests += form
            handler(form)
        }) {
            if (retries) iseuClientConfig(logger = null)
        }))

        val postbacks get() = requests.filter { it.isNotEmpty() }
    }

    private fun MockRequestHandleScope.html(html: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(html, status, headersOf(HttpHeaders.ContentType, "text/html; charset=utf-8"))

    private fun site(
        page: String = fixture("iseu_page.html"),
        timetableGroup: String = "А61ИТ2",
        timetableWeek: String = "12.10.2026",
    ) = Site { form ->
        html(
            when {
                form.isEmpty() -> page
                "ShowTT" in form -> fixture("iseu_timetable.html")
                    .replace("А61ИТ2", timetableGroup)
                    .replace("неделю с 12.10.2026", "неделю с $timetableWeek")
                form["__EVENTTARGET"] == "ddlFac" -> fixture("iseu_page_faculty_4.html")
                form["__EVENTTARGET"] == "ddlCourse" -> fixture("iseu_page_course_2.html")
                else -> error("unexpected postback $form")
            }
        )
    }

    @Test
    fun `serves institute students only`() {
        val source = site().source
        assertTrue(source.serves(firstYear))
        assertFalse(source.serves(User(username = "user", faculty = "Экономический факультет", info = "1 курс, группа 5")))
    }

    @Test
    fun `group of the selected lists costs the page and one postback`() = runBlocking {
        val site = site()
        val week = site.source.load("user", firstYear, tuesday)

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

        assertEquals(listOf(3, 5, 6, 5, 5, 0), week.map { it.size })
    }

    @Test
    fun `next week is requested on sunday`() = runBlocking {
        val site = site(timetableWeek = "19.10.2026")
        site.source.load("user", firstYear, sunday)

        assertEquals("19.10.2026 0:00:00", site.postbacks.last()["ddlWeek"])
    }

    @Test
    fun `current week is shown on sunday when the next one is not published`() = runBlocking {
        val page = fixture("iseu_page.html")
            .replace("<option value=\"19.10.2026 0:00:00\">19.10.2026</option>", "")
        val site = site(page = page)
        site.source.load("user", firstYear, sunday)

        assertEquals("12.10.2026 0:00:00", site.postbacks.last()["ddlWeek"])
    }

    @Test
    fun `week that is not published is empty without a postback`() = runBlocking {
        val site = site()
        val week = site.source.load("user", firstYear, LocalDate(2030, 9, 3))

        assertEquals(6, week.size)
        assertTrue(week.all { it.isEmpty() })
        assertEquals(1, site.requests.size)
    }

    @Test
    fun `faculty is switched by the abbreviation in the profile`() = runBlocking {
        val site = site(timetableGroup = "А61МД2")
        site.source.load(
            "user",
            user("1 курс, группа А61МД2, форма обучения дневная, специальность: лечебное дело ФЭМ"),
            tuesday
        )

        assertEquals(2, site.postbacks.size)
        assertEquals("ddlFac", site.postbacks[0]["__EVENTTARGET"])
        assertEquals("4", site.postbacks[0]["ddlFac"])
        assertEquals("4", site.postbacks[1]["ddlFac"])
        assertEquals("375", site.postbacks[1]["ddlGroup"])
    }

    @Test
    fun `course is switched by the profile and the group is matched loosely`() = runBlocking {
        val site = site(timetableGroup = "А51ИТ2")
        site.source.load(
            "user",
            user("2 курс, группа a51ит2, форма обучения дневная, специальность: информационные системы ФМОС"),
            tuesday
        )

        assertEquals(2, site.postbacks.size)
        assertEquals("ddlCourse", site.postbacks[0]["__EVENTTARGET"])
        assertEquals("2", site.postbacks[0]["ddlCourse"])
        assertEquals("332", site.postbacks[1]["ddlGroup"])
    }

    @Test
    fun `group missing from the lists of the profile is not searched further`() = runBlocking {
        val site = site()
        assertFailsWith<GroupNotFoundException> {
            site.source.load(
                "user",
                user("1 курс, группа А61ИТ9, форма обучения дневная, специальность: информационные системы ФМОС"),
                tuesday
            )
        }
        assertEquals(1, site.requests.size)
    }

    @Test
    fun `profile without a group makes no requests`() = runBlocking {
        val site = site()
        assertFailsWith<GroupNotFoundException> {
            site.source.load("user", user("1 курс, форма обучения дневная"), tuesday)
        }
        assertFailsWith<GroupNotFoundException> {
            site.source.load("user", null, tuesday)
        }
        assertTrue(site.requests.isEmpty())
    }

    @Test
    fun `timetable of another group is rejected`() = runBlocking {
        val site = site(timetableGroup = "А61ИТ1")
        assertFailsWith<IncorrectResponseException> {
            site.source.load("user", firstYear, tuesday)
        }
        Unit
    }

    @Test
    fun `page without the lists is rejected`() = runBlocking {
        val site = site(page = "<html><body>Сервис временно недоступен</body></html>")
        assertFailsWith<IncorrectResponseException> {
            site.source.load("user", firstYear, tuesday)
        }
        Unit
    }

    @Test
    fun `dropped connections are retried`() = runBlocking {
        var drops = 2
        val site = Site(retries = true) { form ->
            if (drops-- > 0)
                throw IOException("unexpected end of stream")
            html(if (form.isEmpty()) fixture("iseu_page.html") else fixture("iseu_timetable.html"))
        }
        val week = site.source.load("user", firstYear, tuesday)

        assertEquals(4, site.requests.size)
        assertEquals(3, week[0].size)
    }

    @Test
    fun `rejected postback is not retried`() = runBlocking {
        val site = Site(retries = true) { form ->
            if (form.isEmpty()) html(fixture("iseu_page.html"))
            else html("Недействительный аргумент обратной передачи", HttpStatusCode.InternalServerError)
        }
        assertFailsWith<FailResponseException> {
            site.source.load("user", firstYear, tuesday)
        }
        assertEquals(2, site.requests.size)
    }
}
