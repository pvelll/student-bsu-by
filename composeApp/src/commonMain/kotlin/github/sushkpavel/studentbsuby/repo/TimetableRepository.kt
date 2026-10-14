package github.sushkpavel.studentbsuby.repo

import github.sushkpavel.studentbsuby.dao.LessonsDao
import github.sushkpavel.studentbsuby.data.models.Lesson
import github.sushkpavel.studentbsuby.data.models.User
import github.sushkpavel.studentbsuby.util.Calendar
import github.sushkpavel.studentbsuby.util.exceptions.GroupNotFoundException
import github.sushkpavel.studentbsuby.util.exceptions.UsernameNotFoundException

fun interface UserProvider {
    suspend fun user(refresh: Boolean): User?
}

class TimetableRepository(
    private val usernameProvider: UsernameProvider,
    private val lessonsDao : LessonsDao,
    private val calendar: Calendar,
    private val userProvider: UserProvider,
    private val bsuSource: TimetableSource,
    private val instituteSources: List<InstituteTimetableSource>,
) : CacheWebRepository<List<List<Lesson>>>() {

    override suspend fun getFromWeb(): List<List<Lesson>> {

        val username = usernameProvider.username.takeIf(String::isNotBlank)
            ?: throw UsernameNotFoundException()

        val today = calendar.today()
        val user = userProvider.user(refresh = false)

        return try {
            sourceFor(user).load(username, user, today)
        } catch (e: GroupNotFoundException) {
            val fresh = userProvider.user(refresh = true)
            if (fresh == null || fresh == user)
                throw e
            sourceFor(fresh).load(username, fresh, today)
        }
    }

    private fun sourceFor(user: User?): TimetableSource =
        user?.let { instituteSources.firstOrNull { source -> source.serves(user) } } ?: bsuSource

    override suspend fun getFromCache(): List<List<Lesson>>{
        val week = shownWeekStart(calendar.today())
        return kotlin.runCatching {
            usernameProvider.username.takeIf(String::isNotBlank)?.let { username ->
                lessonsDao.getAll(username)
            }?.filter {
                it.date.isEmpty() || it.weekStart() == week
            }?.takeIf { it.isNotEmpty() }?.groupBy { it.dayOfWeek }?.toWeek()
        }.getOrNull() ?: emptyList()
    }

    override suspend fun saveToCache(value : List<List<Lesson>> ){
        kotlin.runCatching {
            usernameProvider.username.takeIf(String::isNotBlank)?.let { username ->
                lessonsDao.clear(username)
                value.flatten().forEach {
                    lessonsDao.insert(it)
                }
            }
        }
    }
}
