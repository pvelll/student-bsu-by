package github.sushkpavel.studentbsuby.repo

import github.sushkpavel.studentbsuby.api.FormUrlEncodedBody
import github.sushkpavel.studentbsuby.api.TimetableApi
import github.sushkpavel.studentbsuby.api.isScheduleResponse
import github.sushkpavel.studentbsuby.dao.LessonsDao
import github.sushkpavel.studentbsuby.data.models.Lesson
import github.sushkpavel.studentbsuby.data.models.User
import github.sushkpavel.studentbsuby.util.Calendar
import github.sushkpavel.studentbsuby.util.exceptions.GroupNotFoundException
import github.sushkpavel.studentbsuby.util.exceptions.UsernameNotFoundException
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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

    private class FakeCalendar(private val today: LocalDate) : Calendar {
        override val dayOfMonth = today.day
        override val dayOfWeek = today.dayOfWeek.ordinal
        override val month = today.month.ordinal
        override val year = today.year
        override fun time() = "12:00"
        override fun today() = today
    }

    private class FakeSource(
        private val name: String,
        private val servesUser: (User) -> Boolean = { true },
        private val timetable: (User?) -> List<List<Lesson>> = { listOf(listOf(lesson(0, 1, name = name))) },
    ) : InstituteTimetableSource {
        val loadedFor = mutableListOf<User?>()

        override fun serves(user: User) = servesUser(user)

        override suspend fun load(owner: String, user: User?, today: LocalDate): List<List<Lesson>> {
            loadedFor += user
            return timetable(user)
        }
    }

    private class FakeUserProvider(private val cached: User?, private val fresh: User?) : UserProvider {
        var refreshes = 0

        override suspend fun user(refresh: Boolean): User? {
            if (refresh) refreshes++
            return if (refresh) fresh else cached
        }
    }

    private companion object {
        fun lesson(day: Int, number: Int, date: String = "", name: String = "$day-$number") = Lesson(
            owner = "user", dayOfWeek = day, number = number, name = name,
            place = "", type = "", teacher = "", starts = "", ends = "", date = date
        )
    }

    private val tuesday = LocalDate(2026, 10, 13)

    private val iseuFaculty = "Международный государственный экологический институт имени А.Д.Сахарова"
    private val bsuStudent = User(username = "user", faculty = "Экономический факультет")
    private val iseuStudent = User(username = "user", faculty = iseuFaculty, info = "1 курс, группа А61ИТ2")

    private val bsuSource = FakeSource("bsu")
    private val iseuSource = FakeSource("iseu", servesUser = { it.faculty == iseuFaculty })

    private fun repository(
        vararg lessons: Lesson,
        today: LocalDate = tuesday,
        users: UserProvider = FakeUserProvider(null, null),
        username: String = "user",
        instituteSources: List<InstituteTimetableSource> = listOf(iseuSource),
    ) = TimetableRepository(
        usernameProvider = object : UsernameProvider {
            override val username = username
        },
        lessonsDao = FakeLessonsDao(lessons.toList()),
        calendar = FakeCalendar(today),
        userProvider = users,
        bsuSource = bsuSource,
        instituteSources = instituteSources,
    )

    private suspend fun TimetableRepository.loadedFrom() = getFromWeb().single().single().name

    @Test
    fun `institute students get the timetable of the institute`() = runBlocking {
        assertEquals("iseu", repository(users = FakeUserProvider(iseuStudent, null)).loadedFrom())
        assertEquals(listOf<User?>(iseuStudent), iseuSource.loadedFor)
        assertTrue(bsuSource.loadedFor.isEmpty())
    }

    @Test
    fun `university students and unknown profiles get the timetable of the university`() = runBlocking {
        assertEquals("bsu", repository(users = FakeUserProvider(bsuStudent, null)).loadedFrom())
        assertEquals("bsu", repository(users = FakeUserProvider(null, null)).loadedFrom())
        assertTrue(iseuSource.loadedFor.isEmpty())
    }

    @Test
    fun `first institute serving the student wins`() = runBlocking {
        val other = FakeSource("other", servesUser = { it.faculty == iseuFaculty })
        val repository = repository(
            users = FakeUserProvider(iseuStudent, null),
            instituteSources = listOf(other, iseuSource)
        )
        assertEquals("other", repository.loadedFrom())
    }

    @Test
    fun `outdated profile is refreshed when the group is not found`() = runBlocking {
        val nextYear = iseuStudent.copy(info = "2 курс, группа А61ИТ2")
        val source = FakeSource("iseu", servesUser = { it.faculty == iseuFaculty }, timetable = { user ->
            if (user == iseuStudent) throw GroupNotFoundException()
            listOf(listOf(lesson(0, 1, name = "iseu")))
        })
        val users = FakeUserProvider(cached = iseuStudent, fresh = nextYear)

        assertEquals("iseu", repository(users = users, instituteSources = listOf(source)).loadedFrom())
        assertEquals(1, users.refreshes)
        assertEquals(listOf<User?>(iseuStudent, nextYear), source.loadedFor)
    }

    @Test
    fun `student that left the institute gets the timetable of the university`() = runBlocking {
        val source = FakeSource("iseu", servesUser = { it.faculty == iseuFaculty }, timetable = {
            throw GroupNotFoundException()
        })
        val users = FakeUserProvider(cached = iseuStudent, fresh = bsuStudent)

        assertEquals("bsu", repository(users = users, instituteSources = listOf(source)).loadedFrom())
    }

    @Test
    fun `missing group is reported when the profile is up to date`() = runBlocking {
        val source = FakeSource("iseu", servesUser = { it.faculty == iseuFaculty }, timetable = {
            throw GroupNotFoundException()
        })
        val users = FakeUserProvider(cached = iseuStudent, fresh = iseuStudent)

        assertFailsWith<GroupNotFoundException> {
            repository(users = users, instituteSources = listOf(source)).getFromWeb()
        }
        assertEquals(1, source.loadedFor.size)
    }

    @Test
    fun `other failures do not refresh the profile`() = runBlocking {
        val source = FakeSource("iseu", servesUser = { it.faculty == iseuFaculty }, timetable = {
            throw IOException("connection reset")
        })
        val users = FakeUserProvider(cached = iseuStudent, fresh = iseuStudent.copy(info = "2 курс"))

        assertFailsWith<IOException> {
            repository(users = users, instituteSources = listOf(source)).getFromWeb()
        }
        assertEquals(0, users.refreshes)
    }

    @Test
    fun `timetable is not loaded without a username`() = runBlocking {
        assertFailsWith<UsernameNotFoundException> {
            repository(username = " ", users = FakeUserProvider(iseuStudent, null)).getFromWeb()
        }
        assertTrue(iseuSource.loadedFor.isEmpty())
    }

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
    fun `cached lessons of a past week are outdated`() = runBlocking {
        assertTrue(
            repository(lesson(0, 1, "2026-10-05"), lesson(4, 1, "2026-10-09")).getFromCache().isEmpty()
        )

        val cached = repository(lesson(0, 1, "2026-10-05"), lesson(1, 1, "2026-10-13")).getFromCache()
        assertEquals(6, cached.size)
        assertTrue(cached[0].isEmpty())
        assertEquals(listOf("1-1"), cached[1].map { it.name })
    }

    @Test
    fun `on sunday the cached next week is shown`() = runBlocking {
        val sunday = LocalDate(2026, 10, 18)
        val cached = repository(
            lesson(0, 1, "2026-10-12"), lesson(0, 1, "2026-10-19", name = "next"),
            today = sunday
        ).getFromCache()

        assertEquals(listOf("next"), cached[0].map { it.name })
    }

    @Test
    fun `sunday is cached when it has lessons`() = runBlocking {
        val cached = repository(lesson(0, 1, "2026-10-12"), lesson(6, 1, "2026-10-18")).getFromCache()

        assertEquals(7, cached.size)
        assertEquals(listOf("6-1"), cached[6].map { it.name })
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
