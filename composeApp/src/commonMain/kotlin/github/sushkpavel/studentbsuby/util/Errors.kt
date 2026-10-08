package github.sushkpavel.studentbsuby.util

import github.sushkpavel.studentbsuby.resources.Res
import github.sushkpavel.studentbsuby.resources.error_username_not_found
import github.sushkpavel.studentbsuby.resources.relogin
import github.sushkpavel.studentbsuby.util.exceptions.SessionExpiredException
import github.sushkpavel.studentbsuby.util.exceptions.UsernameNotFoundException
import org.jetbrains.compose.resources.StringResource
import kotlin.coroutines.cancellation.CancellationException

/**
 * User facing message for a data loading failure: the generic message of the screen,
 * unless the cause has a more helpful explanation.
 */
fun Throwable.toErrorMessage(default: StringResource): StringResource = when (this) {
    is UsernameNotFoundException -> Res.string.error_username_not_found
    is SessionExpiredException -> Res.string.relogin
    else -> default
}

/**
 * [runCatching] that does not swallow coroutine cancellation.
 */
inline fun <T> runCatchingSuspend(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (c: CancellationException) {
    throw c
} catch (t: Throwable) {
    Result.failure(t)
}
