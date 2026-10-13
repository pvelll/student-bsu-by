package github.sushkpavel.studentbsuby.api

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Parameters

/**
 * Public timetable of the International Sakharov Environmental Institute of BSU
 * (МГЭИ им. А.Д. Сахарова). Its students use student.bsu.by, but their timetable is
 * published only at rsp.iseu.by.
 *
 * The page is an ASP.NET WebForms form with cascading lists (faculty → form of study →
 * course → group) and a week list. Event validation is on: a postback may only carry the
 * list values rendered on the page it is posted from, so the lists are switched one by one,
 * each postback built from the page returned by the previous one.
 */
interface IseuTimetableApi {

    suspend fun page(): HttpResponse

    suspend fun postback(form: FormUrlEncodedBody): HttpResponse

    companion object {
        // No https: the certificate of the host is issued for another domain.
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
