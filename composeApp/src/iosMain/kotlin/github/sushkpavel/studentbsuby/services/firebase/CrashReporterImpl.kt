package github.sushkpavel.studentbsuby.services.firebase

class CrashReporterImpl : CrashReporter {

    override fun setEnabled(enabled: Boolean) {
        crashlytics?.setCollectionEnabled(enabled)
    }

    override fun recordException(error: Throwable) {
        crashlytics?.record(error)
    }
}
