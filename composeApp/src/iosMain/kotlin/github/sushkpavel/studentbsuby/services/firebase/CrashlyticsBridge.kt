package github.sushkpavel.studentbsuby.services.firebase

import kotlin.concurrent.Volatile
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.getStackTraceAddresses
import kotlin.native.setUnhandledExceptionHook
import kotlin.native.terminateWithUnhandledException

/**
 * Firebase Crashlytics, as seen from Kotlin. The SDK is linked into the Swift app via Swift Package
 * Manager, so the app implements this interface (CrashlyticsBridge.swift) and passes it to
 * [installCrashlytics].
 */
interface CrashlyticsBridge {

    fun setCollectionEnabled(enabled: Boolean)

    /**
     * Records a non-fatal error. [stackTraceAddresses] are return addresses: Crashlytics
     * symbolicates them with the app's dSYM.
     */
    fun recordException(name: String, reason: String, stackTraceAddresses: List<Long>)

    /**
     * Terminates the app with a crash report carrying the Kotlin stack. Never returns.
     */
    fun crash(name: String, reason: String, stackTraceAddresses: List<Long>)
}

@Volatile
internal var crashlytics: CrashlyticsBridge? = null
    private set

/**
 * Called by the Swift app right after `FirebaseApp.configure()`, before any other Kotlin code.
 * Without it (no GoogleService-Info.plist) crash reporting is off.
 */
@OptIn(ExperimentalNativeApi::class)
fun installCrashlytics(bridge: CrashlyticsBridge) {
    crashlytics = bridge
    // By default an uncaught Kotlin exception reaches Crashlytics only as SIGABRT raised by the
    // Kotlin runtime, without the Kotlin stack. The hook has to terminate the app on its own:
    // exceptions from coroutines would be swallowed otherwise.
    setUnhandledExceptionHook { error ->
        error.printStackTrace()
        runCatching {
            bridge.crash(error.crashlyticsName(), error.reasonWithCauses(), error.stackTraceAddressesFromThrowSite())
        }
        terminateWithUnhandledException(error)
    }
}

internal fun CrashlyticsBridge.record(error: Throwable) {
    runCatching {
        recordException(error.crashlyticsName(), error.reasonWithCauses(), error.stackTraceAddressesFromThrowSite())
    }
}

private fun Throwable.crashlyticsName(): String = this::class.qualifiedName ?: "Throwable"

// Crashlytics keeps a single stack per report, so the causes are listed in the reason instead.
private fun Throwable.reasonWithCauses(): String = buildString {
    append(message.orEmpty())
    generateSequence(cause) { it.cause }.take(MAX_CAUSES).forEach {
        append("\nCaused by: ").append(it::class.qualifiedName).append(": ").append(it.message.orEmpty())
    }
}

private const val MAX_CAUSES = 10

/**
 * Kotlin captures the stack in the constructor of Throwable, so the trace starts with the
 * constructors of the exception's class hierarchy. Crashlytics groups issues by the top frames,
 * so these are dropped to put the frame that created the exception on top. Frame names are
 * known only while the binary keeps its symbols (App Store builds are stripped); without them
 * the trace is sent as is.
 */
@OptIn(ExperimentalNativeApi::class)
private fun Throwable.stackTraceAddressesFromThrowSite(): List<Long> {
    val addresses = getStackTraceAddresses()
    val frames = getStackTrace()
    if (frames.size != addresses.size)
        return addresses
    val constructor = "kfun:${this::class.qualifiedName}#<init>"
    return addresses.drop(frames.indexOfLast { constructor in it } + 1)
}
