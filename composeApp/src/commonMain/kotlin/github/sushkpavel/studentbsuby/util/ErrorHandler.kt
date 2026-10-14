package github.sushkpavel.studentbsuby.util

import github.sushkpavel.studentbsuby.services.firebase.CrashReporter
import github.sushkpavel.studentbsuby.util.logger.Logger
import github.sushkpavel.studentbsuby.util.logger.TaggedException
import kotlinx.coroutines.CoroutineExceptionHandler

interface ErrorHandler {

    fun handle(error : Throwable)

    class Ignore : ErrorHandler {
        override fun handle(error: Throwable) = Unit
    }

    class ReThrow : ErrorHandler {
        override fun handle(error: Throwable) = throw error
    }

    /**
     * Expected failures (network, expired session) are turned into DataState.Error by the event
     * handlers, so whatever reaches here is a bug: it is logged and reported to Crashlytics.
     */
    class Log(
        private val logger: Logger,
        private val crashReporter: CrashReporter,
    ) : ErrorHandler {
        override fun handle(error: Throwable) {
            val (tag,cause) = if (error is TaggedException)
                error.message.orEmpty() to error.cause
            else "" to error

            logger.log(
                msg = "Unhandled error occurred",
                tag = tag,
                cause = cause,
                logLevel = Logger.LogLevel.Error
            )
            crashReporter.recordException(error)
        }
    }
}

fun ErrorHandler.toCoroutineExceptionHandler() : CoroutineExceptionHandler =
    CoroutineExceptionHandler { _, throwable ->
        handle(throwable)
    }
