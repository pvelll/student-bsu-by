package github.sushkpavel.studentbsuby.ui.screens.drawer.paidservices

import github.sushkpavel.studentbsuby.data.models.Bill
import github.sushkpavel.studentbsuby.data.models.PaidServicesInfo
import github.sushkpavel.studentbsuby.data.models.Receipt
import github.sushkpavel.studentbsuby.data.models.TuitionFeePayment
import github.sushkpavel.studentbsuby.util.*
import github.sushkpavel.studentbsuby.util.communication.StateCommunication
import github.sushkpavel.studentbsuby.util.dispatchers.Dispatchers

class PaidServicesViewModel(
    override val isUpdating : StateCommunication<Boolean>,
    val paidInfoCommunication : StateCommunication<DataState<PaidServicesInfo>>,
    val paidInfoBillsCommunication : StateCommunication<DataState<List<Bill>>>,
    val hostelBillsCommunication : StateCommunication<DataState<List<Bill>>>,
    val academDebtReceiptsCommunication : StateCommunication<DataState<List<Receipt>>>,
    val commonReceiptsCommunication : StateCommunication<DataState<List<Receipt>>>,
    val tutionFeeCommunication: StateCommunication<DataState<List<TuitionFeePayment>>>,
    eventHandler : PaidServicesEventHandler,
    errorHandler: ErrorHandler,
    dispatchers: Dispatchers,
) : SuspendHandlerViewModel<PaidServicesEvent>(
    dispatchers = dispatchers,
    errorHandler = errorHandler,
    suspendEventHandler = eventHandler
), Updatable {

    override fun update() {
        handle(PaidServicesEvent.UpdateRequested)
    }
}
