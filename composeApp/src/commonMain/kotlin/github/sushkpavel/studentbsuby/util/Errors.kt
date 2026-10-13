package github.sushkpavel.studentbsuby.util

import github.sushkpavel.studentbsuby.resources.Res
import github.sushkpavel.studentbsuby.resources.error_group_not_found
import github.sushkpavel.studentbsuby.resources.error_username_not_found
import github.sushkpavel.studentbsuby.resources.relogin
import github.sushkpavel.studentbsuby.util.exceptions.GroupNotFoundException
import github.sushkpavel.studentbsuby.util.exceptions.SessionExpiredException
import github.sushkpavel.studentbsuby.util.exceptions.UsernameNotFoundException
import org.jetbrains.compose.resources.StringResource
import kotlin.coroutines.cancellation.CancellationException

fun Throwable.toErrorMessage(default: StringResource): StringResource = when (this) {
    is UsernameNotFoundException -> Res.string.error_username_not_found
    is SessionExpiredException -> Res.string.relogin
    is GroupNotFoundException -> Res.string.error_group_not_found
    else -> default
}

inline fun <T> runCatchingSuspend(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (c: CancellationException) {
    throw c
} catch (t: Throwable) {
    Result.failure(t)
}
