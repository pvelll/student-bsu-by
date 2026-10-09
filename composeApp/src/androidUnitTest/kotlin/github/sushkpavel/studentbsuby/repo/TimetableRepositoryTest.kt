package github.sushkpavel.studentbsuby.repo

import github.sushkpavel.studentbsuby.api.FormUrlEncodedBody
import github.sushkpavel.studentbsuby.api.TimetableApi
import github.sushkpavel.studentbsuby.api.isScheduleResponse
import github.sushkpavel.studentbsuby.dao.LessonsDao
import github.sushkpavel.studentbsuby.data.models.Lesson
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TimetableRepositoryTest {

    private val api = object : TimetableApi {
        override suspend fun init(): HttpResponse = error("not used")
        override suspend fun timetable(dayOfWeek: FormUrlEncodedBody): HttpResponse = error("not used")
    }

    private class FakeLessonsDao(val lessons: List<Lesson>) : LessonsDao {
        override suspend fun insert(value: Lesson) = Unit
        override suspend fun getAll(username: String) = lessons.filter { it.owner == username }
        override suspend fun clear(username: String) = Unit
    }

    private fun lesson(day: Int, number: Int) = Lesson(
        owner = "user", dayOfWeek = day, number = number, name = "$day-$number",
        place = "", type = "", teacher = "", starts = "", ends = ""
    )

    private fun repository(vararg lessons: Lesson) = TimetableRepository(
        usernameProvider = object : UsernameProvider {
            override val username = "user"
        },
        timetableApi = api,
        lessonsDao = FakeLessonsDao(lessons.toList())
    )

    @Test
    fun `cached lessons stay on their days when some days are free`() = runBlocking {
        val cached = repository(lesson(0, 1), lesson(0, 2), lesson(3, 1), lesson(5, 4))
            .getFromCache()

        assertEquals(6, cached.size)
        assertEquals(listOf("0-1", "0-2"), cached[0].map { it.name })
        assertTrue(cached[1].isEmpty())
        assertTrue(cached[2].isEmpty())
        assertEquals(listOf("3-1"), cached[3].map { it.name })
        assertTrue(cached[4].isEmpty())
        assertEquals(listOf("5-4"), cached[5].map { it.name })
    }

    @Test
    fun `nothing is returned when nothing is cached`() = runBlocking {
        assertTrue(repository().getFromCache().isEmpty())
    }

    @Test
    fun `rejected weekday postback is not a schedule`() {
        val day = "1|#||4|120|updatePanel|ctl00_ctl00_ContentPlaceHolder0_ContentPlaceHolder1_ctlSchedule1_UpdatePanel1|<table></table>|"
        val redirect = "1|#||4|0|pageRedirect||%2fError.aspx|"

        assertTrue(api.isScheduleResponse(day))
        assertFalse(api.isScheduleResponse(redirect))
        assertFalse(api.isScheduleResponse("<html><body>Ошибка</body></html>"))
    }
}
