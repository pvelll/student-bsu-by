package github.sushkpavel.studentbsuby.di

import github.sushkpavel.studentbsuby.ui.screens.drawer.about.AboutEventHandlerImpl
import github.sushkpavel.studentbsuby.ui.screens.drawer.about.AboutViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

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
