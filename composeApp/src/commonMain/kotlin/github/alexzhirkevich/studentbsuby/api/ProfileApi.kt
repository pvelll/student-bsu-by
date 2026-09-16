package github.alexzhirkevich.studentbsuby.api

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Parameters

interface ProfileApi {

    suspend fun photo() : HttpResponse

    suspend fun studProgress(): HttpResponse

    /**
     * Postback of the StudProgress page. [form] must be built with [AspNetForm] from a
     * freshly loaded page: the server validates the view state of every postback.
     */
    suspend fun subjects(form : FormUrlEncodedBody) : HttpResponse

    suspend fun newsItem(id : Int) : HttpResponse

    suspend fun news(): HttpResponse

    suspend fun hostel(): HttpResponse

    suspend fun studBilet(): HttpResponse

    suspend fun exit() : HttpResponse

    companion object{
        const val URL_NEWS = "PersonalCabinet/News"
        const val URL_STUD_PROGRESS = "PersonalCabinet/StudProgress"

        /** `__doPostBack` target of the "Все сессии" link of the marks page. */
        const val ALL_SESSIONS_EVENT_TARGET =
            "ctl00\$ctl00\$ContentPlaceHolder0\$ContentPlaceHolder1\$ctlStudProgress1\$selSemester"

        /** Id suffix of the marks table. */
        const val PROGRESS_TABLE_ID = "ctlStudProgress1_tblProgress"
    }
}

class ProfileApiImpl(private val client : HttpClient) : ProfileApi {

    override suspend fun photo(): HttpResponse =
        client.get("Photo/Photo.aspx")

    override suspend fun studProgress(): HttpResponse =
        client.get(ProfileApi.URL_STUD_PROGRESS)

    override suspend fun subjects(form: FormUrlEncodedBody): HttpResponse =
        client.submitForm(
            url = ProfileApi.URL_STUD_PROGRESS,
            formParameters = Parameters.build {
                form.forEach { (k, v) -> append(k, v) }
            }
        ) {
            header("Referer", "https://student.bsu.by/${ProfileApi.URL_STUD_PROGRESS}")
        }

    override suspend fun newsItem(id: Int): HttpResponse =
        client.get(ProfileApi.URL_NEWS) {
            parameter("id", id)
        }

    override suspend fun news(): HttpResponse =
        client.get(ProfileApi.URL_NEWS)

    override suspend fun hostel(): HttpResponse =
        client.get("PersonalCabinet/Hostel")

    override suspend fun studBilet(): HttpResponse =
        client.get("PersonalCabinet/stb")

    override suspend fun exit(): HttpResponse =
        throw UnsupportedOperationException()
}
