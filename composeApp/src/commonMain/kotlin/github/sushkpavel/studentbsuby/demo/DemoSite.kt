package github.sushkpavel.studentbsuby.demo

import github.sushkpavel.studentbsuby.api.AspNetForm
import github.sushkpavel.studentbsuby.api.ProfileApi
import github.sushkpavel.studentbsuby.resources.Res
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.http.parseQueryString
import io.ktor.http.withCharset
import io.ktor.utils.io.charsets.Charsets
import kotlinx.coroutines.delay
import kotlin.random.Random

class DemoResponse(
    val status: HttpStatusCode,
    val headers: Headers,
    val body: ByteArray,
)

/**
 * In-process stand-in for student.bsu.by used by the demo mode: it answers with pages of the made up
 * [DemoData] student, so the api wrappers, parsers, caches, cookies and the login flow run unchanged.
 *
 * The session lives in the [AUTH_COOKIE] cookie like on the real site: the login postback sets it,
 * the logout (cookies cleanup) drops it, and without it the cabinet pages answer with the login form.
 */
class DemoSite(private val latency: LongRange = 250L..700L) {

    private var captcha = 0

    suspend fun respond(method: HttpMethod, url: Url, headers: Headers, body: ByteArray): DemoResponse {
        if (!latency.isEmpty())
            delay(Random.nextLong(latency.first, latency.last + 1))

        val path = url.encodedPath.lowercase().trimEnd('/')
        val post = method == HttpMethod.Post
        val form = if (post) parseQueryString(body.decodeToString()) else Parameters.Empty
        val cookies = headers[HttpHeaders.Cookie].orEmpty()
        val authorized = "$AUTH_COOKIE=" in cookies

        return when {
            path == "/login.aspx" -> when {
                post -> login(form)
                authorized -> html(DemoPages.loggedIn())
                SESSION_COOKIE in cookies -> html(DemoPages.login())
                else -> html(DemoPages.login(), "$SESSION_COOKIE=${token()}; path=/; HttpOnly")
            }

            path == "/captcha/captchaimage.aspx" ->
                image(Res.readBytes("files/demo/captcha_${captcha++ % CAPTCHAS}.png"), ContentType.Image.PNG)

            !authorized -> html(DemoPages.login())

            path == "/photo/photo.aspx" ->
                image(Res.readBytes("files/demo/photo.jpg"), ContentType.Image.JPEG)

            path == "/${ProfileApi.URL_STUD_PROGRESS}".lowercase() ->
                html(DemoPages.studProgress(if (post) progressSession(form) else DemoData.currentSession))

            path == "/${ProfileApi.URL_NEWS}".lowercase() -> {
                val id = url.parameters["id"]
                if (id == null) html(DemoPages.news())
                else id.toIntOrNull()?.let(DemoPages::newsItem)?.let { html(it) } ?: notFound()
            }

            path == "/personalcabinet/schedule" -> if (post) scheduleDay(form) else html(DemoPages.schedule())

            path == "/personalcabinet/hostel" -> html(DemoPages.hostel())

            path == "/personalcabinet/stb" -> html(DemoPages.studentCard())

            path == "/personalcabinet/pay/info" -> html(DemoPages.payInfo())
            path == "/personalcabinet/pay/report" -> html(DemoPages.tuitionFee())
            path == "/personalcabinet/pay/reportakadem" -> html(DemoPages.academicDebt())
            path == "/personalcabinet/pay/housepay" -> html(DemoPages.hostelBills())
            path == "/personalcabinet/pay/reportother" -> html(DemoPages.commonReceipts())

            else -> notFound()
        }
    }

    // Only the demo account gets here (see DemoAccount), so the captcha is not checked.
    private fun login(form: Parameters): DemoResponse {
        if (form[LOGIN_FIELD].isNullOrBlank() || form[PASSWORD_FIELD].isNullOrBlank())
            return html(DemoPages.login(error = "Неверный логин или пароль"))

        return DemoResponse(
            status = HttpStatusCode.Found,
            headers = Headers.build {
                append(HttpHeaders.Location, "/${ProfileApi.URL_NEWS}")
                append(HttpHeaders.SetCookie, "$AUTH_COOKIE=${token()}; path=/; HttpOnly")
            },
            body = ByteArray(0)
        )
    }

    /** Session requested by a postback of the marks page, null for "Все сессии". */
    private fun progressSession(form: Parameters): Int? {
        val target = form[AspNetForm.EVENT_TARGET].orEmpty()
        if (form[AspNetForm.VIEW_STATE] != DemoPages.VIEW_STATE || target.isEmpty())
            return DemoData.currentSession
        if (target == ProfileApi.ALL_SESSIONS_EVENT_TARGET)
            return null
        return target.substringAfterLast("selSemester").toIntOrNull()
            ?.minus(1)
            ?.takeIf { it in DemoData.sessions.indices }
            ?: DemoData.currentSession
    }

    private fun scheduleDay(form: Parameters): DemoResponse {
        val day = form[AspNetForm.EVENT_TARGET].orEmpty()
            .substringAfterLast("cmdDay", "").toIntOrNull()
            ?.minus(1)
            ?.takeIf { it in DemoData.weekdays.indices }

        return text(
            if (day != null && form[AspNetForm.VIEW_STATE] == DemoPages.VIEW_STATE)
                DemoPages.scheduleDelta(day) else DemoPages.ERROR_REDIRECT_DELTA,
            ContentType.Text.Plain
        )
    }

    private fun html(content: String, setCookie: String? = null): DemoResponse =
        text(content, ContentType.Text.Html, setCookie)

    private fun text(content: String, type: ContentType, setCookie: String? = null) = DemoResponse(
        status = HttpStatusCode.OK,
        headers = Headers.build {
            append(HttpHeaders.ContentType, type.withCharset(Charsets.UTF_8).toString())
            setCookie?.let { append(HttpHeaders.SetCookie, it) }
        },
        body = content.encodeToByteArray()
    )

    private fun image(bytes: ByteArray, type: ContentType) = DemoResponse(
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, type.toString()),
        body = bytes
    )

    private fun notFound() = DemoResponse(HttpStatusCode.NotFound, Headers.Empty, ByteArray(0))

    private fun token(): String = buildString {
        repeat(24) { append(TOKEN_CHARS[Random.nextInt(TOKEN_CHARS.length)]) }
    }

    private companion object {
        const val AUTH_COOKIE = "AuthCookie"
        const val SESSION_COOKIE = "ASP.NET_SessionId"
        const val LOGIN_FIELD = "ctl00\$ContentPlaceHolder0\$txtUserLogin"
        const val PASSWORD_FIELD = "ctl00\$ContentPlaceHolder0\$txtUserPassword"
        const val TOKEN_CHARS = "0123456789abcdefghijklmnopqrstuvwxyz"
        const val CAPTCHAS = 4
    }
}
