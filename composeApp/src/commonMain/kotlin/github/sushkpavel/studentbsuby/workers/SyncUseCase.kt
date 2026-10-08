package github.sushkpavel.studentbsuby.workers

import github.sushkpavel.studentbsuby.data.models.Lesson
import github.sushkpavel.studentbsuby.repo.CacheWebRepository
import github.sushkpavel.studentbsuby.repo.HostelRepository
import github.sushkpavel.studentbsuby.repo.HostelState
import github.sushkpavel.studentbsuby.repo.LoginRepository
import github.sushkpavel.studentbsuby.repo.TimetableRepository
import github.sushkpavel.studentbsuby.resources.Res
import github.sushkpavel.studentbsuby.resources.hostel
import github.sushkpavel.studentbsuby.resources.hostel_provided
import github.sushkpavel.studentbsuby.resources.timetable
import github.sushkpavel.studentbsuby.resources.timetable_changed
import github.sushkpavel.studentbsuby.resources.weekdays
import github.sushkpavel.studentbsuby.util.NotificationCreator
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getStringArray

private const val HostelUpdateNotification  = 1000001
private const val TimetableUpdateNotification  = 1000092

enum class SyncResult {
    Success,
    Retry
}

class SyncUseCase(
    private val hostelRepository: HostelRepository,
    private val timetableRepository: TimetableRepository,
    private val loginRepository: LoginRepository,
    private val notificationCreator: NotificationCreator,
) {

    suspend fun performSync(): SyncResult {
        return if (login()) {
            timetableRepository.init()
            update().also {
                if (!loginRepository.autoLogin)
                    loginRepository.logout()
            }
        }
        else SyncResult.Retry
    }

    private suspend fun login() : Boolean = kotlin.runCatching {
        val res = loginRepository.initialize()
        if (res.loggedIn)
            return true

        repeat(5) {
            loginRepository.updateCaptcha(false)?.let { bmp ->
                loginRepository.getCaptchaText(bmp)
            }?.let {
                loginRepository.login(loginRepository.username, loginRepository.password, it)
            }?.let {
                if (it.loggedIn)
                    return true
            }
        }
        return false
    }.getOrNull() == true


    private suspend fun update()  = coroutineScope {
        try {
            listOf(
                async { update(hostelRepository, ::notifyHostelChanged) },
                async { update(timetableRepository, ::notifyTimetableChanged) }
            ).awaitAll()
            SyncResult.Success
        } catch (t: Throwable) {
            SyncResult.Retry
        }
    }

    private suspend fun <T> update(
        repo : CacheWebRepository<T>,
        notify : suspend (T,T) -> Unit
    ) = with(repo) {
        val cached = getFromCache() ?: return@with
        val new = getFromWeb() ?: return@with

        if (cached != new){
            notify(cached,new)
            saveToCache(new)
        }
    }

    private suspend fun notifyHostelChanged(old: HostelState, new: HostelState) {
        when {
            old is HostelState.NotProvided && new is HostelState.Provided -> {
                notificationCreator.sendNotification(
                    id = HostelUpdateNotification,
                    sub = getString(Res.string.hostel),
                    title = getString(Res.string.hostel_provided),
                    text = new.address
                )
            }
        }
    }

    private suspend fun notifyTimetableChanged(old : List<List<Lesson>>, new : List<List<Lesson>>) {

        val weekdays = getStringArray(Res.array.weekdays)

        val changedWeekdays = old.zip(new).mapIndexedNotNull { index, pair ->
            if (pair.first != pair.second)
                weekdays[index] else null
        }
        if (changedWeekdays.isNotEmpty()) {
            notificationCreator.sendNotification(
                id = TimetableUpdateNotification,
                sub = getString(Res.string.timetable),
                title= getString(Res.string.timetable_changed),
                text = changedWeekdays.joinToString(separator = ", ")
            )
        }
    }
}
