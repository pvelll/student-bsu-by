package github.sushkpavel.studentbsuby.api

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Parameters

interface IseuTimetableApi {

    suspend fun page(): HttpResponse

    suspend fun postback(form: FormUrlEncodedBody): HttpResponse

    companion object {
        const val URL = "http://rsp.iseu.by/Raspisanie/TimeTable/umu.aspx"
    }
}

class IseuTimetableApiImpl(private val client: HttpClient) : IseuTimetableApi {

    override suspend fun page(): HttpResponse =
        client.get(IseuTimetableApi.URL)

    override suspend fun postback(form: FormUrlEncodedBody): HttpResponse =
        client.submitForm(
            url = IseuTimetableApi.URL,
            formParameters = Parameters.build {
                form.forEach { (k, v) -> append(k, v) }
            }
        ) {
            header("Referer", IseuTimetableApi.URL)
        }
}
