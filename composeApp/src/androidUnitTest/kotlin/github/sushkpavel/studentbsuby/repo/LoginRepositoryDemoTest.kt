package github.sushkpavel.studentbsuby.repo

import com.russhwolf.settings.MapSettings
import github.sushkpavel.studentbsuby.api.LoginApiImpl
import github.sushkpavel.studentbsuby.api.LoginApiWrapper
import github.sushkpavel.studentbsuby.demo.DemoAccount
import github.sushkpavel.studentbsuby.demo.DemoMode
import github.sushkpavel.studentbsuby.demo.DemoRoutingEngine
import github.sushkpavel.studentbsuby.demo.DemoSite
import github.sushkpavel.studentbsuby.network.bsuClientConfig
import github.sushkpavel.studentbsuby.util.CaptchaRecognizer
import github.sushkpavel.studentbsuby.util.PersistentCookiesStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoginRepositoryDemoTest {

    private val demoLogin = "reviewer"
    private val demoPassword = "Secret-Pass-1"

    private val demoMode = DemoMode(MapSettings())
    private val cookies = PersistentCookiesStorage(MapSettings())
    private val networkRequests = mutableListOf<Url>()

    // student.bsu.by as seen from outside of Belarus
    private val unreachableSite = MockEngine { request ->
        networkRequests += request.url
        respond(
            """<span id="ctl00_ContentPlaceHolder0_lbLoginResult">Неверный логин или пароль</span>""",
            headers = headersOf(HttpHeaders.ContentType, "text/html; charset=utf-8")
        )
    }

    private val repository = LoginRepository(
        api = LoginApiWrapper(LoginApiImpl(HttpClient(DemoRoutingEngine(unreachableSite, DemoSite(LongRange.EMPTY), demoMode)) {
            bsuClientConfig(cookies, logger = null)
        })),
        credentialsPreferences = MapSettings(),
        captchaRecognizer = object : CaptchaRecognizer {
            override suspend fun recognize(image: androidx.compose.ui.graphics.ImageBitmap) = ""
        },
        loginCookieManager = cookies,
        demoAccount = DemoAccount(DemoAccount.digest(demoLogin, demoPassword)),
        demoMode = demoMode,
    )

    @Test
    fun `the demo account signs in without the site and without a captcha`() = runBlocking {
        val response = repository.login("Reviewer", demoPassword, captcha = "")

        assertTrue(response.loggedIn)
        assertTrue(demoMode.isEnabled)
        assertEquals("Reviewer", repository.username)
        assertTrue(networkRequests.isEmpty())
    }

    @Test
    fun `the demo session is restored on the next start`() = runBlocking {
        repository.login(demoLogin, demoPassword, captcha = "")

        assertTrue(repository.initialize().loggedIn)
        assertTrue(networkRequests.isEmpty())
    }

    @Test
    fun `logout leaves the demo mode`() = runBlocking {
        repository.login(demoLogin, demoPassword, captcha = "")
        repository.logout()

        assertFalse(demoMode.isEnabled)
        assertFalse(cookies.canRestoreSession())
    }

    @Test
    fun `other credentials go to the site and leave the demo mode`() = runBlocking {
        repository.login(demoLogin, demoPassword, captcha = "")

        val response = repository.login("1234567", demoPassword, captcha = "123456")

        assertFalse(response.loggedIn)
        assertFalse(demoMode.isEnabled)
        assertFalse(cookies.canRestoreSession())
        assertEquals(1, networkRequests.size)
    }
}
