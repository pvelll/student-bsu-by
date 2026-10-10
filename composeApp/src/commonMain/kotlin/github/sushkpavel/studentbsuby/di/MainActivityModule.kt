package github.sushkpavel.studentbsuby.di

import github.sushkpavel.studentbsuby.MainActivityEventHandlerImpl
import github.sushkpavel.studentbsuby.MainActivityViewModel
import github.sushkpavel.studentbsuby.repo.RemoteConfigRepository
import github.sushkpavel.studentbsuby.repo.ReviewRepository
import github.sushkpavel.studentbsuby.repo.UpdateRepository
import github.sushkpavel.studentbsuby.util.communication.StateCommunication
import github.sushkpavel.studentbsuby.util.communication.StateFlowCommunication
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val mainModule = module {

    single<StateCommunication<Boolean>>(named("ShowUpdate")) { StateFlowCommunication(false) }

    factory { RemoteConfigRepository(get(), get(), get()) }

    factory { UpdateRepository(get()) }

    factory { ReviewRepository(get(), get()) }

    viewModel {
        val showUpdate = get<StateCommunication<Boolean>>(named("ShowUpdate"))
        val eventHandler = MainActivityEventHandlerImpl(
            dispatchers = get(),
            remoteConfigRepository = get(),
            updateRepository = get(),
            reviewRepository = get(),
            showUpdateRequired = showUpdate as StateFlowCommunication<Boolean>,
            platformActions = get()
        )
        MainActivityViewModel(
            showUpdateDialog = showUpdate,
            dispatchers = get(),
            errorHandler = get(),
            eventHandler = eventHandler
        )
    }
}
