package github.sushkpavel.studentbsuby.di

import github.sushkpavel.studentbsuby.repo.HostelRepository
import github.sushkpavel.studentbsuby.repo.HostelState
import github.sushkpavel.studentbsuby.ui.screens.drawer.hostel.HostelEventHandlerImpl
import github.sushkpavel.studentbsuby.ui.screens.drawer.hostel.HostelViewModel
import github.sushkpavel.studentbsuby.util.DataState
import github.sushkpavel.studentbsuby.util.communication.StateFlowCommunication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Mirrors the original HostelModule (ViewModelComponent).
 */
val hostelModule = module {

    // Unqualified ObservableSettings resolves the default preferences file
    // (the original injected unqualified SharedPreferences = default prefs).
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
