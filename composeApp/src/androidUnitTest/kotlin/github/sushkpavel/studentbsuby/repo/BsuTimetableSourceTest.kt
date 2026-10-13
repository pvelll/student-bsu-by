package github.sushkpavel.studentbsuby.repo

import github.sushkpavel.studentbsuby.api.TimetableApiImpl
import github.sushkpavel.studentbsuby.api.TimetableApiWrapper
import github.sushkpavel.studentbsuby.network.bsuClientConfig
import github.sushkpavel.studentbsuby.util.exceptions.IncorrectResponseException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BsuTimetableSourceTest {

    private companion object {
        const val PAGE = -1
    }

    private val schedulePage = """
        <html><body><form id="aspnetForm">
        <input type="hidden" name="__VIEWSTATE" id="__VIEWSTATE" value="/wEPDwUKLTM1NTg2" />
        <input type="hidden" name="__EVENTVALIDATION" id="__EVENTVALIDATION" value="/wEdAAmB" />
        <a id="ctl00_ctl00_ContentPlaceHolder0_ContentPlaceHolder1_ctlSchedule1_cmdDay1"
           href="javascript:__doPostBack('cmdDay1','')">Понедельник</a>
        </form></body></html>
    """.trimIndent()

    private fun dayResponse(day: Int): String {
        val table = """<table><tr><td class="styleNumber">1</td><td class="styleTimeIn">08:15</td>""" +
                """<td class="styleTimeOut">09:35</td><td class="styleLesson">Предмет $day</td>""" +
                """<td class="styleKindLesson">лек.</td><td class="styleHall">501</td>""" +
                """<td class="styleTeacher">Иванов И.И.</td></tr></table>"""
        return "1|#||4|${table.length}|updatePanel|" +
                "ctl00_ctl00_ContentPlaceHolder0_ContentPlaceHolder1_ctlSchedule1_UpdatePanel1|$table|"
    }

    private class Site(
        private val handler: suspend MockRequestHandleScope.(day: Int, attempt: Int) -> HttpResponseData,
    ) {
        private val attempts = ConcurrentHashMap<Int, AtomicInteger>()

        fun attempts(day: Int) = attempts[day]?.get() ?: 0

        val source = BsuTimetableSource(TimetableApiWrapper(TimetableApiImpl(HttpClient(MockEngine { request ->
            val day = (request.body as? FormDataContent)?.formData?.get("__EVENTTARGET")
                ?.substringAfterLast("cmdDay")?.toInt()?.minus(1) ?: PAGE
            handler(day, attempts.computeIfAbsent(day) { AtomicInteger() }.incrementAndGet())
        }) {
            bsuClientConfig(AcceptAllCookiesStorage(), logger = null)
        })))
    }

    private fun MockRequestHandleScope.html(html: String) =
        respond(html, headers = headersOf(HttpHeaders.ContentType, "text/html; charset=utf-8"))

    private fun MockRequestHandleScope.page(day: Int) =
        html(if (day == PAGE) schedulePage else dayResponse(day))

    private val tuesday = LocalDate(2026, 10, 13)

    private suspend fun Site.week() = source.load("user", null, tuesday)

    @Test
    fun `day that timed out on a mobile network is requested again`() = runBlocking {
        val site = Site { day, attempt ->
            if (day == 3 && attempt == 1)
                throw SocketTimeoutException("Socket timeout has expired")
            page(day)
        }
        val week = site.week()

        assertEquals((0..5).map { listOf("Предмет $it") }, week.map { day -> day.map { it.name } })
        assertEquals(2, site.attempts(3))
        assertEquals(1, site.attempts(2))
    }

    @Test
    fun `schedule page is requested again after a dropped connection`() = runBlocking {
        val site = Site { day, attempt ->
            if (day == PAGE && attempt == 1)
                throw IOException("unexpected end of stream")
            page(day)
        }

        assertEquals(6, site.week().count { it.isNotEmpty() })
        assertEquals(2, site.attempts(PAGE))
    }

    @Test
    fun `day that keeps failing fails the week instead of showing it partially`() = runBlocking {
        val site = Site { day, _ ->
            if (day == 3) throw IOException("connection reset")
            page(day)
        }

        assertFailsWith<IOException> { site.week() }
        assertEquals(3, site.attempts(3))
    }

    @Test
    fun `day rejected by the server is not requested again`() = runBlocking {
        val site = Site { day, _ ->
            if (day == 3) html("1|#||4|0|pageRedirect||%2fError.aspx|") else page(day)
        }

        assertFailsWith<IncorrectResponseException> { site.week() }
        assertEquals(1, site.attempts(3))
    }
}
