package github.sushkpavel.studentbsuby.ui.screens.drawer.progress

import github.sushkpavel.studentbsuby.util.DataState
import github.sushkpavel.studentbsuby.util.ErrorHandler
import github.sushkpavel.studentbsuby.util.SuspendHandlerViewModel
import github.sushkpavel.studentbsuby.util.Updatable
import github.sushkpavel.studentbsuby.util.communication.StateCommunication
import github.sushkpavel.studentbsuby.util.dispatchers.Dispatchers

class ProgressViewModel(
    override val isUpdating: StateCommunication<Boolean>,
    val progressCommunication: StateCommunication<DataState<AcademicProgress>>,
    eventHandler: ProgressEventHandler,
    errorHandler: ErrorHandler,
    dispatchers: Dispatchers
) : SuspendHandlerViewModel<ProgressEvent>(
    suspendEventHandler = eventHandler,
    errorHandler = errorHandler,
    dispatchers = dispatchers
), Updatable {

    override fun update() {
        handle(ProgressEvent.UpdateRequested)
    }
}
