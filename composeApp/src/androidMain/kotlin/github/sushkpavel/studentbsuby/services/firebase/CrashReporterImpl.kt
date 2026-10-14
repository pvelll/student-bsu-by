package github.sushkpavel.studentbsuby.services.firebase

import com.google.firebase.crashlytics.FirebaseCrashlytics

class CrashReporterImpl : CrashReporter {

    override fun setEnabled(enabled: Boolean) {
        kotlin.runCatching {
            FirebaseCrashlytics.getInstance().run {
                setCrashlyticsCollectionEnabled(enabled)
                // Reports collected before the opt-out would otherwise be sent once it is re-enabled.
                if (!enabled) deleteUnsentReports()
            }
        }
    }

    override fun recordException(error: Throwable) {
        kotlin.runCatching {
            FirebaseCrashlytics.getInstance().recordException(error)
        }
    }
}
