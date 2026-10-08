package github.sushkpavel.studentbsuby.di

import github.sushkpavel.studentbsuby.ui.screens.drawer.about.AboutEventHandlerImpl
import github.sushkpavel.studentbsuby.ui.screens.drawer.about.AboutViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Mirrors the original AboutModule (ViewModelComponent).
 * RemoteConfigRepository (consumed by the About event handler) is bound in [mainModule].
 */
val aboutModule = module {
    viewModel {
        val eventHandler = AboutEventHandlerImpl(
            platformActions = get(),
            configRepository = get()
        )
        AboutViewModel(
            errorHandler = get(),
            dispatchers = get(),
            eventHandler = eventHandler
        )
    }
}
