package github.sushkpavel.studentbsuby.util

import github.sushkpavel.studentbsuby.util.communication.Releasable
import github.sushkpavel.studentbsuby.util.communication.StateCommunication

interface ConnectivityManager : Releasable {

    val isNetworkConnected: StateCommunication<Boolean>
}

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
