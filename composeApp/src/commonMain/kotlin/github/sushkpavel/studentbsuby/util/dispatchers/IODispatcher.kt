package github.sushkpavel.studentbsuby.util.dispatchers

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

interface IODispatcher {

    fun launchIO(
        scope: CoroutineScope,
        key: Any? = null,
        exceptionHandler: CoroutineExceptionHandler? = null,
        block: suspend CoroutineScope.() -> Unit,
    ): Job

    suspend fun <T> runOnIO(
        exceptionHandler: CoroutineExceptionHandler? = null,
        block: suspend CoroutineScope.() -> T
    ): T
}

internal expect val IoDispatcher: CoroutineDispatcher
