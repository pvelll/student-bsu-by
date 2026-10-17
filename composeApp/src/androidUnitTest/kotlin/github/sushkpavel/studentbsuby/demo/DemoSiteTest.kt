package github.sushkpavel.studentbsuby.demo

import com.russhwolf.settings.MapSettings
import github.sushkpavel.studentbsuby.api.LoginApiImpl
import github.sushkpavel.studentbsuby.api.LoginApiWrapper
import github.sushkpavel.studentbsuby.api.PaidServicesApiImpl
import github.sushkpavel.studentbsuby.api.ProfileApiImpl
import github.sushkpavel.studentbsuby.api.TimetableApiImpl
import github.sushkpavel.studentbsuby.api.TimetableApiWrapper
import github.sushkpavel.studentbsuby.api.createLoginData
import github.sushkpavel.studentbsuby.data.models.Receipt
import github.sushkpavel.studentbsuby.network.bsuClientConfig
import github.sushkpavel.studentbsuby.repo.BsuTimetableSource
import github.sushkpavel.studentbsuby.repo.HostelRepository
import github.sushkpavel.studentbsuby.repo.HostelState
import github.sushkpavel.studentbsuby.repo.NewsContentRepository
import github.sushkpavel.studentbsuby.repo.NewsRepository
import github.sushkpavel.studentbsuby.repo.PaidServicesParser
import github.sushkpavel.studentbsuby.repo.SubjectsRepository
import github.sushkpavel.studentbsuby.repo.UserRepository
import github.sushkpavel.studentbsuby.repo.UsernameProvider
import github.sushkpavel.studentbsuby.repo.html
import github.sushkpavel.studentbsuby.util.exceptions.SessionExpiredException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import java.lang.reflect.Proxy
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Store reviewers see the app only through the demo mode: every section of the demo site has to
 * be read by the same api wrappers and parsers as the real one, otherwise the review fails.
 */
class DemoSiteTest {

    private val owner = "reviewer"

    private val usernameProvider = object : UsernameProvider {
        override val username = owner
    }

    private val client = HttpClient(
        DemoRoutingEngine(
            network = MockEngine { error("The demo mode reached the network: ${it.url}") },
            site = DemoSite(latency = LongRange.EMPTY),
            demoMode = DemoMode(MapSettings()).apply { enable() }
        )
    ) {
        bsuClientConfig(AcceptAllCookiesStorage(), logger = null)
    }

    private val profileApi = ProfileApiImpl(client)
    private val paidServicesApi = PaidServicesApiImpl(client)

    @BeforeTest
    fun signIn() = runBlocking {
        val loginApi = LoginApiWrapper(LoginApiImpl(client))
        loginApi.initialize()
        val response = loginApi.login(loginApi.createLoginData(owner, "password", "123456"))
        assertEquals(HttpStatusCode.Found, response.status)
    }

    @Test
    fun `cabinet pages ask to sign in without a session`() = runBlocking {
        val anonymous = HttpClient(
            DemoRoutingEngine(MockEngine { error("network") }, DemoSite(LongRange.EMPTY), DemoMode(MapSettings()).apply { enable() })
        ) {
            bsuClientConfig(AcceptAllCookiesStorage(), logger = null)
        }
        assertFailsWith<SessionExpiredException> { ProfileApiImpl(anonymous).studProgress().html() }
        Unit
    }

    @Test
    fun profile() = runBlocking {
        val user = UserRepository(unused(), profileApi, usernameProvider).getFromWeb()

        assertEquals(DemoData.NAME, user.name)
        assertEquals(DemoData.FACULTY, user.faculty)
        assertEquals(DemoData.INFO, user.info)
        assertEquals(DemoData.averageText, user.avgGrade)
    }

    @Test
    fun `marks of every session`() = runBlocking {
        val sessions = SubjectsRepository(usernameProvider, profileApi, unused()).getFromWeb()

        assertEquals(DemoData.sessions.map { it.subjects.map { s -> s.name } }, sessions.map { it.map { s -> s.name } })
    }

    @Test
    fun timetable() = runBlocking {
        val week = BsuTimetableSource(TimetableApiWrapper(TimetableApiImpl(client)))
            .load(owner, null, LocalDate(2026, 10, 13))

        assertEquals(
            DemoData.timetable.map { day -> day.map { it.name to it.starts } },
            week.map { day -> day.map { it.name to it.starts } }
        )
    }

    @Test
    fun news() = runBlocking {
        val news = NewsRepository(profileApi, unused(), "https://student.bsu.by").getFromWeb()

        assertEquals(DemoData.news.map { it.id to it.title }, news.orEmpty().map { it.id to it.title })

        val item = DemoData.news.first()
        val content = NewsContentRepository(item.id, unused(), profileApi).getFromWeb()
        assertContains(content.content, item.title)
    }

    @Test
    fun hostel() = runBlocking {
        val hostel = HostelRepository(usernameProvider, profileApi, unused(), MapSettings()).getFromWeb()

        assertEquals(HostelState.Provided(DemoData.HOSTEL_ADDRESS), hostel)
    }

    @Test
    fun `paid services`() = runBlocking {
        val (info, bills) = PaidServicesParser.parseInfo(paidServicesApi.info().html(), owner)!!
        assertEquals(DemoData.CONTRACT.substringAfter("№ "), info.contractNumber)
        assertEquals(DemoData.bills.size, bills.size)

        assertEquals(
            DemoData.tuitionFee.size,
            PaidServicesParser.parseTuitionFees(paidServicesApi.tuitionFee().html(), owner)?.size
        )
        assertEquals(
            DemoData.hostelBills.size,
            PaidServicesParser.parseHostelBills(paidServicesApi.hostelBills().html(), owner, "hostel")?.size
        )
        assertEquals(
            DemoData.commonReceipts.size,
            PaidServicesParser.parseReceipts(paidServicesApi.common().html(), owner, Receipt.TYPE_COMMON)?.size
        )
        assertEquals(
            0,
            PaidServicesParser.parseReceipts(paidServicesApi.academicDebt().html(), owner, Receipt.TYPE_ACADEM_DEBT)?.size
        )
    }

    // The repositories read their caches only when the site fails.
    private inline fun <reified T : Any> unused(): T =
        Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
            throw UnsupportedOperationException(method.name)
        } as T
}
