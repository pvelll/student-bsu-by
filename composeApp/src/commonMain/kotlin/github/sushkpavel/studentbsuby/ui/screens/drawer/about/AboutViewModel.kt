package github.sushkpavel.studentbsuby.ui.screens.drawer.about

import github.sushkpavel.studentbsuby.util.dispatchers.Dispatchers
import github.sushkpavel.studentbsuby.util.ErrorHandler
import github.sushkpavel.studentbsuby.util.SuspendHandlerViewModel

class AboutViewModel(
    errorHandler: ErrorHandler,
    dispatchers: Dispatchers,
    eventHandler: IAboutEventHandler
) : SuspendHandlerViewModel<AboutEvent>(
    errorHandler = errorHandler,
    dispatchers = dispatchers,
    suspendEventHandler = eventHandler
)
