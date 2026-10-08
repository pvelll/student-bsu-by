package github.sushkpavel.studentbsuby

import github.sushkpavel.studentbsuby.util.dispatchers.Dispatchers
import github.sushkpavel.studentbsuby.util.ErrorHandler
import github.sushkpavel.studentbsuby.util.SuspendHandlerViewModel
import github.sushkpavel.studentbsuby.util.communication.StateCommunication


class MainActivityViewModel(
    val showUpdateDialog : StateCommunication<Boolean>,
    dispatchers: Dispatchers,
    errorHandler: ErrorHandler,
    eventHandler: MainActivityEventHandler
) : SuspendHandlerViewModel<MainActivityEvent>(
    dispatchers = dispatchers,
    suspendEventHandler = eventHandler,
    errorHandler = errorHandler
)
