package github.sushkpavel.studentbsuby.di

import github.sushkpavel.studentbsuby.repo.BsuTimetableSource
import github.sushkpavel.studentbsuby.repo.DataSource
import github.sushkpavel.studentbsuby.repo.IseuTimetableSource
import github.sushkpavel.studentbsuby.repo.TimetableRepository
import github.sushkpavel.studentbsuby.repo.UserProvider
import github.sushkpavel.studentbsuby.repo.UserRepository
import github.sushkpavel.studentbsuby.ui.screens.drawer.timetable.Timetable
import github.sushkpavel.studentbsuby.ui.screens.drawer.timetable.TimetableEventHandlerImpl
import github.sushkpavel.studentbsuby.ui.screens.drawer.timetable.TimetableViewModel
import github.sushkpavel.studentbsuby.util.DataState
import github.sushkpavel.studentbsuby.util.communication.StateFlowCommunication
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.lastOrNull
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val timetableModule = module {

    factory { BsuTimetableSource(get()) }

    factory { IseuTimetableSource(get()) }

    factory {
        val userRepository = get<UserRepository>()
        TimetableRepository(
            usernameProvider = get(),
            lessonsDao = get(),
            calendar = get(),
            userProvider = UserProvider { refresh ->
                val cached = if (refresh) null
                else userRepository.get(DataSource.Local).firstOrNull()
                cached ?: userRepository.get(DataSource.Remote).lastOrNull()
            },
            bsuSource = get<BsuTimetableSource>(),
            instituteSources = listOf(get<IseuTimetableSource>()),
        )
    }

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
