import FirebaseCrashlytics
import ComposeApp

/// Passes Kotlin errors to Firebase Crashlytics (see CrashlyticsBridge.kt).
final class FirebaseCrashlyticsBridge: NSObject, CrashlyticsBridge {

    func setCollectionEnabled(enabled: Bool) {
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(enabled)
        // Reports collected before the opt-out would otherwise be sent once it is re-enabled.
        if !enabled {
            Crashlytics.crashlytics().deleteUnsentReports()
        }
    }

    func recordException(name: String, reason: String, stackTraceAddresses: [KotlinLong]) {
        let exception = ExceptionModel(name: name, reason: reason)
        exception.stackTrace = stackTraceAddresses.map {
            StackFrame(address: UInt(truncatingIfNeeded: $0.int64Value))
        }
        Crashlytics.crashlytics().record(exceptionModel: exception)
    }

    func crash(name: String, reason: String, stackTraceAddresses: [KotlinLong]) {
        // record(exceptionModel:) writes the report later on the main queue, which never happens
        // once the app terminates. An uncaught NSException is written by Crashlytics right away.
        KotlinException(name: name, reason: reason, stackTraceAddresses: stackTraceAddresses).raise()
    }
}

/// Crashlytics takes the frames of an uncaught NSException from callStackReturnAddresses,
/// so they are replaced with the stack of the Kotlin exception.
private final class KotlinException: NSException {

    private let stackTraceAddresses: [NSNumber]

    init(name: String, reason: String, stackTraceAddresses: [NSNumber]) {
        self.stackTraceAddresses = stackTraceAddresses
        super.init(name: NSExceptionName(name), reason: reason, userInfo: nil)
    }

    required init?(coder: NSCoder) {
        return nil
    }

    override var callStackReturnAddresses: [NSNumber] {
        stackTraceAddresses
    }
}
