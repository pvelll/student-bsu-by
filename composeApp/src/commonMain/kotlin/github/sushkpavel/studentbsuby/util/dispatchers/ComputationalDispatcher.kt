package github.sushkpavel.studentbsuby.util.dispatchers

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

interface ComputationalDispatcher {

    fun launchComputation(
        scope: CoroutineScope,
        key: Any? = null,
        exceptionHandler: CoroutineExceptionHandler? = null,
        block: suspend CoroutineScope.() -> Unit,
    ): Job

    suspend fun <T> runOnComputational(
        exceptionHandler: CoroutineExceptionHandler? = null,
        block: suspend CoroutineScope.() -> T
    ): T
}
