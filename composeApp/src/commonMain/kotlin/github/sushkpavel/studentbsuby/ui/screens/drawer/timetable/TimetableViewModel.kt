package github.sushkpavel.studentbsuby.ui.screens.drawer.timetable

import github.sushkpavel.studentbsuby.util.*
import github.sushkpavel.studentbsuby.util.communication.StateCommunication
import github.sushkpavel.studentbsuby.util.dispatchers.Dispatchers

class TimetableViewModel(
    override val isUpdating: StateCommunication<Boolean>,
    val timetableCommunication: StateCommunication<DataState<Timetable>>,
    dispatchers: Dispatchers,
    errorHandler: ErrorHandler,
    eventHandler: TimetableEventHandler,
    calendar: Calendar
): SuspendHandlerViewModel<TimetableEvent>(
    dispatchers = dispatchers,
    suspendEventHandler = eventHandler,
    errorHandler = errorHandler
), Updatable, Calendar by calendar {

    override fun update() {
        handle(TimetableEvent.UpdateRequested)
    }
}
