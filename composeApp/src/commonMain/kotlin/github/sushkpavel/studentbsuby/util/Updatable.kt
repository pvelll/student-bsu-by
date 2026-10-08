package github.sushkpavel.studentbsuby.util

import github.sushkpavel.studentbsuby.util.communication.StateCommunication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface Updatable {

    val isUpdating : StateCommunication<Boolean>

    fun update()
}

suspend fun setState(block : () -> Unit) = withContext(Dispatchers.Main){block()}
