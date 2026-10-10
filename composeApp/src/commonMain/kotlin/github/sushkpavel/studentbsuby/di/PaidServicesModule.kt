package github.sushkpavel.studentbsuby.di

import github.sushkpavel.studentbsuby.data.models.Bill
import github.sushkpavel.studentbsuby.data.models.PaidServicesInfo
import github.sushkpavel.studentbsuby.data.models.Receipt
import github.sushkpavel.studentbsuby.data.models.TuitionFeePayment
import github.sushkpavel.studentbsuby.repo.PaidServicesRepository
import github.sushkpavel.studentbsuby.ui.screens.drawer.paidservices.PaidServicesEventHandlerImpl
import github.sushkpavel.studentbsuby.ui.screens.drawer.paidservices.PaidServicesViewModel
import github.sushkpavel.studentbsuby.util.DataState
import github.sushkpavel.studentbsuby.util.communication.StateFlowCommunication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val paidServicesModule = module {

    factory { PaidServicesRepository(get(), get(), get(), get()) }

    viewModel {
        val isUpdating = StateFlowCommunication(false)
        val paidInfoBills = StateFlowCommunication<DataState<List<Bill>>>(DataState.Loading)
        val hostelBills = StateFlowCommunication<DataState<List<Bill>>>(DataState.Loading)
        val academDebtReceipts = StateFlowCommunication<DataState<List<Receipt>>>(DataState.Loading)
        val commonReceipts = StateFlowCommunication<DataState<List<Receipt>>>(DataState.Loading)
        val paidInfo = StateFlowCommunication<DataState<PaidServicesInfo>>(DataState.Loading)
        val tutionFee = StateFlowCommunication<DataState<List<TuitionFeePayment>>>(DataState.Loading)

        val eventHandler = PaidServicesEventHandlerImpl(
            paidServicesRepository = get(),
            connectivityManager = get(),
            isUpdatingMapper = isUpdating,
            paidInfoBillsMapper = paidInfoBills,
            hostelBillsMapper = hostelBills,
            academDebtReceiptsMapper = academDebtReceipts,
            commonReceiptsMapper = commonReceipts,
            tutionFeePaymentsMapper = tutionFee,
            paidInfoMapper = paidInfo,
            platformActions = get()
        )

        PaidServicesViewModel(
            isUpdating = isUpdating,
            paidInfoCommunication = paidInfo,
            paidInfoBillsCommunication = paidInfoBills,
            hostelBillsCommunication = hostelBills,
            academDebtReceiptsCommunication = academDebtReceipts,
            commonReceiptsCommunication = commonReceipts,
            tutionFeeCommunication = tutionFee,
            eventHandler = eventHandler,
            errorHandler = get(),
            dispatchers = get()
        )
    }
}
