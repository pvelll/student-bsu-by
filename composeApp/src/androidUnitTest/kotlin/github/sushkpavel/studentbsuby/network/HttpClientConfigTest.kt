package github.sushkpavel.studentbsuby.network

import github.sushkpavel.studentbsuby.api.LoginApiImpl
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.get
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.headersOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.util.Collections
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HttpClientConfigTest {

    private class Server(
        config: HttpClientConfig<*>.() -> Unit,
        private val handler: suspend MockRequestHandleScope.(HttpRequestData, attempt: Int) -> HttpResponseData,
    ) {
        val requests: MutableList<HttpRequestData> = Collections.synchronizedList(mutableListOf())

        val client = HttpClient(MockEngine { request ->
            requests += request
            handler(request, requests.size)
        }, config)
    }

    private val bsu: HttpClientConfig<*>.() -> Unit = { bsuClientConfig(AcceptAllCookiesStorage(), logger = null) }
    private val iseu: HttpClientConfig<*>.() -> Unit = { iseuClientConfig(logger = null) }

    @Test
    fun `hung request is abandoned and sent again within its own timeout`() = runBlocking {
        for (config in listOf(bsu, iseu)) {
            val server = Server(config) { _, attempt ->
                if (attempt == 1) delay(10_000)
                respond("ok")
            }
            val response = server.client.get("https://student.bsu.by/PersonalCabinet/Schedule") {
                timeout { requestTimeoutMillis = 300 }
            }

            assertEquals("ok", response.bodyAsText())
            assertEquals(2, server.requests.size)
        }
    }

    @Test
    fun `gateway failures of pages are retried`() = runBlocking {
        val server = Server(bsu) { _, attempt ->
            if (attempt == 1) respond("", HttpStatusCode.BadGateway) else respond("ok")
        }

        assertEquals("ok", server.client.get("PersonalCabinet/StudProgress").bodyAsText())
        assertEquals(2, server.requests.size)
    }

    @Test
    fun `forms are not sent twice unless the request allows it`() = runBlocking {
        val server = Server(bsu) { _, _ -> respond("", HttpStatusCode.BadGateway) }
        server.client.submitForm("PersonalCabinet/StudProgress", Parameters.Empty)

        assertEquals(1, server.requests.size)
    }

    @Test
    fun `login form is never sent twice`() = runBlocking {
        val server = Server(bsu) { _, _ -> throw IOException("connection reset") }

        assertFailsWith<IOException> {
            LoginApiImpl(server.client).login(mapOf("ctl00\$ContentPlaceHolder0\$txtPassword" to "secret"))
        }
        assertEquals(1, server.requests.size)
    }

    @Test
    fun `debug log keeps the password and the session cookie out`() = runBlocking {
        val log = Collections.synchronizedList(mutableListOf<String>())
        val logger = object : Logger {
            override fun log(message: String) {
                log += message
            }
        }
        val server = Server({ bsuClientConfig(AcceptAllCookiesStorage(), logger) }) { request, _ ->
            if (request.url.encodedPath.endsWith("login.aspx"))
                respond("", HttpStatusCode.Found, headersOf(HttpHeaders.SetCookie, ".ASPXAUTH=session-token; path=/"))
            else
                respond("<html>schedule</html>")
        }
        val login = LoginApiImpl(server.client)
        login.initialize()
        login.login(mapOf("ctl00\$ContentPlaceHolder0\$txtPassword" to "very-secret-password"))
        server.client.get("PersonalCabinet/Schedule").bodyAsText()

        val text = log.joinToString("\n")
        assertTrue("PersonalCabinet/Schedule" in text)
        assertTrue(server.requests.last().headers[HttpHeaders.Cookie].orEmpty().contains("session-token"))
        assertFalse("very-secret-password" in text)
        assertFalse("session-token" in text)
    }
}
