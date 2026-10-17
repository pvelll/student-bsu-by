package github.sushkpavel.studentbsuby.demo

import com.russhwolf.settings.MapSettings
import github.sushkpavel.studentbsuby.network.bsuClientConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DemoRoutingEngineTest {

    private val settings = MapSettings()
    private val demoMode = DemoMode(settings)
    private val networkRequests = mutableListOf<Url>()

    private val client = HttpClient(
        DemoRoutingEngine(
            network = MockEngine { request ->
                networkRequests += request.url
                respond("network", headers = headersOf(HttpHeaders.ContentType, "text/html"))
            },
            site = DemoSite(latency = LongRange.EMPTY),
            demoMode = demoMode
        )
    ) {
        bsuClientConfig(AcceptAllCookiesStorage(), logger = null)
    }

    @Test
    fun `requests go to the network outside of the demo mode`() = runBlocking {
        assertEquals("network", client.get("login.aspx").bodyAsText())
        assertEquals(listOf("https://student.bsu.by/login.aspx"), networkRequests.map(Url::toString))
    }

    @Test
    fun `the demo mode is served without the network`() = runBlocking {
        demoMode.enable()

        assertContains(client.get("login.aspx").bodyAsText(), "txtUserLogin")
        assertTrue(networkRequests.isEmpty())
    }

    @Test
    fun `requests go to the network again after the demo mode`() = runBlocking {
        demoMode.enable()
        client.get("login.aspx")
        demoMode.disable()

        assertEquals("network", client.get("login.aspx").bodyAsText())
        assertEquals(1, networkRequests.size)
    }

    @Test
    fun `the demo mode is restored after a restart`() {
        demoMode.enable()
        assertTrue(DemoMode(settings).isEnabled)

        demoMode.disable()
        assertFalse(DemoMode(settings).isEnabled)
    }
}
