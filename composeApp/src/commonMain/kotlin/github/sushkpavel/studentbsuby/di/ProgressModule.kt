package github.sushkpavel.studentbsuby.di

import github.sushkpavel.studentbsuby.ui.screens.drawer.progress.AcademicProgress
import github.sushkpavel.studentbsuby.ui.screens.drawer.progress.ProgressEventHandlerImpl
import github.sushkpavel.studentbsuby.ui.screens.drawer.progress.ProgressViewModel
import github.sushkpavel.studentbsuby.util.DataState
import github.sushkpavel.studentbsuby.util.communication.StateFlowCommunication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val progressModule = module {

    viewModel {
        val isUpdating = StateFlowCommunication(false)
        val progress = StateFlowCommunication<DataState<AcademicProgress>>(DataState.Loading)

        val eventHandler = ProgressEventHandlerImpl(
            subjectsRepository = get(),
            currentSemesterRepository = get(),
            connectivityManager = get(),
            isUpdatingMapper = isUpdating,
            progressMapper = progress
        )

        ProgressViewModel(
            isUpdating = isUpdating,
            progressCommunication = progress,
            eventHandler = eventHandler,
            errorHandler = get(),
            dispatchers = get()
        )
    }
}
