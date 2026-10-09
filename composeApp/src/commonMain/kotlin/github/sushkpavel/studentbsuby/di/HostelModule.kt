package github.sushkpavel.studentbsuby.di

import github.sushkpavel.studentbsuby.repo.HostelRepository
import github.sushkpavel.studentbsuby.repo.HostelState
import github.sushkpavel.studentbsuby.ui.screens.drawer.hostel.HostelEventHandlerImpl
import github.sushkpavel.studentbsuby.ui.screens.drawer.hostel.HostelViewModel
import github.sushkpavel.studentbsuby.util.DataState
import github.sushkpavel.studentbsuby.util.communication.StateFlowCommunication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val hostelModule = module {

    factory { HostelRepository(get(), get(), get(), get()) }

    viewModel {
        val hostelCommunication = StateFlowCommunication<DataState<HostelState>>(DataState.Loading)
        val isUpdatingCommunication = StateFlowCommunication(false)

        val eventHandler = HostelEventHandlerImpl(
            hostelRepository = get(),
            connectivityManager = get(),
            isUpdatingMapper = isUpdatingCommunication,
            hostelStateMapper = hostelCommunication,
            platformActions = get()
        )

        HostelViewModel(
            isUpdating = isUpdatingCommunication,
            hostelStateCommunication = hostelCommunication,
            hostelRepository = get(),
            errorHandler = get(),
            dispatchers = get(),
            eventHandler = eventHandler
        )
    }
}
