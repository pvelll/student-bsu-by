package github.alexzhirkevich.studentbsuby.repo

import com.russhwolf.settings.ObservableSettings
import github.alexzhirkevich.studentbsuby.api.AspNetForm
import github.alexzhirkevich.studentbsuby.api.ProfileApi
import github.alexzhirkevich.studentbsuby.dao.SubjectsDao
import github.alexzhirkevich.studentbsuby.data.models.Subject
import github.alexzhirkevich.studentbsuby.util.exceptions.IncorrectResponseException
import github.alexzhirkevich.studentbsuby.util.exceptions.UsernameNotFoundException
import github.alexzhirkevich.studentbsuby.util.runCatchingSuspend
import kotlinx.coroutines.flow.Flow

private const val PREF_CURRENTSEMESTER_ = "PREF_CURRENTSEMESTER_"

class SubjectsRepository(
    private val usernameProvider: UsernameProvider,
    private val profileApi: ProfileApi,
    private val subjectsDao: SubjectsDao,
) : CacheWebRepository<List<List<Subject>>>() {

    override fun get(
        dataSource: DataSource,
        replaceCacheIf: (cached: List<List<Subject>>?, new: List<List<Subject>>) -> Boolean
    ): Flow<List<List<Subject>>> = super.get(dataSource){ old, new ->
        replaceCacheIf(old,new) && new.isNotEmpty()
    }

    /**
     * Loads the marks of all sessions.
     *
     * The page renders only the current session by default; all sessions are requested
     * with the "Все сессии" postback, which must carry the view state of a freshly
     * loaded page. If that postback fails for any reason the current session from the
     * loaded page is returned instead, so the screen degrades instead of breaking.
     */
    override suspend fun getFromWeb(): List<List<Subject>> {
        val username = usernameProvider.username

        if (username.isEmpty())
            throw UsernameNotFoundException()

        val currentSessionPage = profileApi.studProgress().html()

        val allSessionsPage = runCatchingSuspend {
            val form = AspNetForm.parse(currentSessionPage)
            if (!form.isValid)
                throw IncorrectResponseException()
            profileApi.subjects(form.postback(ProfileApi.ALL_SESSIONS_EVENT_TARGET)).html()
        }.getOrNull()?.takeIf { it.contains(ProfileApi.PROGRESS_TABLE_ID) }

        val page = allSessionsPage ?: currentSessionPage

        if (!page.contains(ProfileApi.PROGRESS_TABLE_ID))
            throw IncorrectResponseException()

        return SubjectsParser.parse(page, username)
    }

    override suspend fun saveToCache(value: List<List<Subject>>) {
        kotlin.runCatching {
            val subjects = value.flatten()
            if (subjects.isNotEmpty()) {
                subjectsDao.clear(subjects[0].owner)
                subjects.forEach {
                    subjectsDao.insert(it)
                }
            }
        }
    }

    override suspend fun getFromCache(): List<List<Subject>>? {
        return kotlin.runCatching {
            usernameProvider
                .username.takeIf(String::isNotEmpty)?.let { login ->
                    subjectsDao
                        .getAll(login)
                        .groupBy { it.semester }
                        .values.toList()
                }
        }.getOrNull()
    }
}

class CurrentSemesterRepository(
    private val usernameProvider: UsernameProvider,
    private val profileApi: ProfileApi,
    private val sharedPreferences: ObservableSettings
) : CacheWebRepository<Int>() {
    override suspend fun getFromCache(): Int? {
        return kotlin.runCatching {
            val username = usernameProvider.username
            if (username.isNotEmpty()) {
                val semester = sharedPreferences.getInt(PREF_CURRENTSEMESTER_ + username, -1)
                semester.takeIf { it >= 0 }
            } else null
        }.getOrNull()
    }

    override suspend fun getFromWeb(): Int? {
        val page = profileApi.studProgress().html()
        return SubjectsParser.currentSemesterIndex(page)
    }

    override suspend fun saveToCache(value: Int) {
        kotlin.runCatching {
            val username = usernameProvider.username
            if (username.isNotEmpty()) {
                sharedPreferences.putInt(PREF_CURRENTSEMESTER_ + username, value)
            }
        }
    }
}
