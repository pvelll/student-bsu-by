package github.sushkpavel.studentbsuby.api

import com.fleeksoft.ksoup.Ksoup
import github.sushkpavel.studentbsuby.util.exceptions.FailResponseException
import github.sushkpavel.studentbsuby.util.exceptions.IncorrectResponseException
import github.sushkpavel.studentbsuby.util.exceptions.SessionExpiredException
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.isSuccess

private const val SCHEDULE_CONTROL = "ctl00\$ctl00\$ContentPlaceHolder0\$ContentPlaceHolder1\$ctlSchedule1"

fun TimetableApi.dayOfWeek(day : Int) : FormUrlEncodedBody = mapOf(
    "$SCHEDULE_CONTROL\$ScriptManager1" to "$SCHEDULE_CONTROL\$UpdatePanel1|$SCHEDULE_CONTROL\$cmdDay${day+1}",
    AspNetForm.EVENT_TARGET to "$SCHEDULE_CONTROL\$cmdDay${day+1}",
)

fun TimetableApi.isScheduleResponse(response: String): Boolean =
    response.contains("ctlSchedule1") && !response.contains("|pageRedirect|")

interface TimetableApi {

    suspend fun init() : HttpResponse

    suspend fun timetable(dayOfWeek: FormUrlEncodedBody) : HttpResponse

    val hasSchedule: Boolean get() = true
}

class TimetableApiImpl(private val client : HttpClient) : TimetableApi {

    override suspend fun init(): HttpResponse =
        client.get("PersonalCabinet/Schedule") {
            header("User-Agent", "Mozilla")
        }

    override suspend fun timetable(dayOfWeek: FormUrlEncodedBody): HttpResponse =
        client.submitForm(
            url = "PersonalCabinet/Schedule",
            formParameters = Parameters.build {
                dayOfWeek.forEach { (k, v) -> append(k, v) }
            }
        ) {
            header("User-Agent", "Mozilla")
        }
}

class TimetableApiWrapper(private val api : TimetableApi) : TimetableApi{

    private var form: AspNetForm = AspNetForm.parse("")

    override var hasSchedule: Boolean = true
        private set

    override suspend fun init(): HttpResponse {
        val resp = api.init()

        if (!resp.status.isSuccess())
            throw FailResponseException(resp.status.value)

        val body = resp.bodyAsText()
        if (body.isSessionExpired())
            throw SessionExpiredException()

        val document = Ksoup.parse(body)
        val form = AspNetForm.parse(document)
        if (!form.isValid)
            throw IncorrectResponseException()

        this.form = form
        val dayLinks = document.select("a[id*=cmdDay]")
        hasSchedule = dayLinks.isEmpty() || dayLinks.any { it.hasAttr("href") }
        return resp
    }

    override suspend fun timetable(dayOfWeek: FormUrlEncodedBody): HttpResponse {
        val eventTarget = dayOfWeek[AspNetForm.EVENT_TARGET].orEmpty()
        val body = form.postback(eventTarget, extra = dayOfWeek) +
                (AspNetForm.ASYNC_POST to "true")
        return api.timetable(body)
    }
}
