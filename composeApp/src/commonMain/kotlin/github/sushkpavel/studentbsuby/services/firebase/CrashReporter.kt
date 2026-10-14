package github.sushkpavel.studentbsuby.services.firebase

interface CrashReporter {

    fun setEnabled(enabled: Boolean)

    /**
     * Sends a non-fatal report: the error was caught, but nothing in the app expected it.
     */
    fun recordException(error: Throwable)
}
