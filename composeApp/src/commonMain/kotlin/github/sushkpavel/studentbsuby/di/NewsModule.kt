package github.sushkpavel.studentbsuby.di

import github.sushkpavel.studentbsuby.data.models.News
import github.sushkpavel.studentbsuby.repo.NewsRepository
import github.sushkpavel.studentbsuby.ui.screens.drawer.news.NewsEventHandlerImpl
import github.sushkpavel.studentbsuby.ui.screens.drawer.news.NewsViewModel
import github.sushkpavel.studentbsuby.util.DataState
import github.sushkpavel.studentbsuby.util.communication.StateFlowCommunication
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val newsModule = module {

    factory {
        NewsRepository(
            api = get(),
            dao = get(),
            baseUrl = get(named("BaseUrl")),
        )
    }

    viewModel {
        val newsCommunication = StateFlowCommunication<DataState<List<News>>>(DataState.Loading)
        val isUpdatingCommunication = StateFlowCommunication(false)

        val eventHandler = NewsEventHandlerImpl(
            newsRepository = get(),
            connectivityManager = get(),
            isUpdatingMapper = isUpdatingCommunication,
            newsMapper = newsCommunication
        )

        NewsViewModel(
            isUpdating = isUpdatingCommunication,
            newsCommunication = newsCommunication,
            errorHandler = get(),
            dispatchers = get(),
            eventHandler = eventHandler
        )
    }
}
