package github.alexzhirkevich.studentbsuby.util

import github.alexzhirkevich.studentbsuby.util.communication.Releasable
import github.alexzhirkevich.studentbsuby.util.communication.StateCommunication

interface ConnectivityManager : Releasable {

    val isNetworkConnected: StateCommunication<Boolean>
}

/**
 * Runs [block] every time the network becomes available again.
 *
 * The state is a hot state flow, so the very first value received is the current
 * connectivity, not a change: it is skipped, otherwise every screen loaded its data twice
 * on start (once directly, once as a reaction to the "connected" state).
 */
suspend fun ConnectivityManager.onReconnected(block: suspend () -> Unit) {
    var first = true
    isNetworkConnected.collect { connected ->
        if (first) {
            first = false
            return@collect
        }
        if (connected) {
            block()
        }
    }
}
