package github.sushkpavel.studentbsuby.ui.screens.drawer.hostel

import github.sushkpavel.studentbsuby.repo.HostelRepository
import github.sushkpavel.studentbsuby.repo.HostelState
import github.sushkpavel.studentbsuby.util.*
import github.sushkpavel.studentbsuby.util.communication.StateCommunication
import github.sushkpavel.studentbsuby.util.dispatchers.Dispatchers

class HostelViewModel(
    override val isUpdating: StateCommunication<Boolean>,
    val hostelStateCommunication: StateCommunication<DataState<HostelState>>,
    private val hostelRepository: HostelRepository,
    errorHandler: ErrorHandler,
    dispatchers: Dispatchers,
    eventHandler: HostelEventHandler
) : SuspendHandlerViewModel<HostelEvent>(
    dispatchers = dispatchers,
    errorHandler = errorHandler,
    suspendEventHandler = eventHandler
), Updatable {

    override fun update() = handle(HostelEvent.UpdateRequested)

    fun getHostelImage(value: HostelState.Provided): String =
        hostelRepository.getImageForHostel(
            hostelRepository.getHostelNumber(value.address)
        )
}
