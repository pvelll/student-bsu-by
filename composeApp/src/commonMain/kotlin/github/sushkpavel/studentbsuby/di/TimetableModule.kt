package github.sushkpavel.studentbsuby.di

import github.sushkpavel.studentbsuby.repo.TimetableRepository
import github.sushkpavel.studentbsuby.ui.screens.drawer.timetable.Timetable
import github.sushkpavel.studentbsuby.ui.screens.drawer.timetable.TimetableEventHandlerImpl
import github.sushkpavel.studentbsuby.ui.screens.drawer.timetable.TimetableViewModel
import github.sushkpavel.studentbsuby.util.DataState
import github.sushkpavel.studentbsuby.util.communication.StateFlowCommunication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val timetableModule = module {

    factory { TimetableRepository(get(), get(), get()) }

    viewModel {
        val isUpdating = StateFlowCommunication(false)
        val timetable = StateFlowCommunication<DataState<Timetable>>(DataState.Loading)

        val eventHandler = TimetableEventHandlerImpl(
            timetableRepository = get(),
            loginRepository = get(),
            connectivityManager = get(),
            calendar = get(),
            timetableMapper = timetable,
            isUpdatingMapper = isUpdating
        )

        TimetableViewModel(
            isUpdating = isUpdating,
            timetableCommunication = timetable,
            dispatchers = get(),
            errorHandler = get(),
            eventHandler = eventHandler,
            calendar = get()
        )
    }
}
