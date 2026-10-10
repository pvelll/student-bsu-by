package github.sushkpavel.studentbsuby.util.dispatchers

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

interface UIDispatcher{

    fun launchUI(
        scope: CoroutineScope,
        key: Any? = null,
        exceptionHandler: CoroutineExceptionHandler? = null,
        block: suspend CoroutineScope.() -> Unit
    ): Job

    suspend fun <T> runOnUI(
        exceptionHandler: CoroutineExceptionHandler? = null,
        block: suspend CoroutineScope.() -> T
    ): T
}
