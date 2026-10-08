package github.sushkpavel.studentbsuby.ui.screens.drawer.news

import github.sushkpavel.studentbsuby.resources.Res
import github.sushkpavel.studentbsuby.resources.error_load_news
import github.sushkpavel.studentbsuby.data.models.News
import github.sushkpavel.studentbsuby.repo.DataSource
import github.sushkpavel.studentbsuby.repo.NewsRepository
import github.sushkpavel.studentbsuby.util.ConnectivityManager
import github.sushkpavel.studentbsuby.util.onReconnected
import github.sushkpavel.studentbsuby.util.DataState
import github.sushkpavel.studentbsuby.util.BaseSuspendEventHandler
import github.sushkpavel.studentbsuby.util.SuspendEventHandler
import github.sushkpavel.studentbsuby.util.communication.Mapper
import github.sushkpavel.studentbsuby.util.communication.StateMapper
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

class NewsEventHandlerImpl(
    newsRepository: NewsRepository,
    connectivityManager: ConnectivityManager,
    isUpdatingMapper: Mapper<Boolean>,
    newsMapper: StateMapper<DataState<List<News>>>
) : NewsEventHandler, SuspendEventHandler<NewsEvent> by SuspendEventHandler.from(
    UpdateRequestedHandler(
        connectivityManager = connectivityManager,
        newsRepository = newsRepository,
        isUpdatingMapper = isUpdatingMapper,
        newsMapper = newsMapper
    )
)

private class UpdateRequestedHandler(
    private val connectivityManager: ConnectivityManager,
    private val newsRepository: NewsRepository,
    private val isUpdatingMapper : Mapper<Boolean>,
    private val newsMapper : StateMapper<DataState<List<News>>>
) : BaseSuspendEventHandler<NewsEvent.UpdateRequested>(
    NewsEvent.UpdateRequested::class
) {

    override suspend fun launch() {
        isUpdatingMapper.map(false)
        newsMapper.map(DataState.Loading)
        update(DataSource.All)
        connectivityManager.onReconnected {
            update(DataSource.Remote)
        }
    }

    override suspend fun handle(event: NewsEvent.UpdateRequested) {
        isUpdatingMapper.map(true)
        update(DataSource.Remote)
        isUpdatingMapper.map(false)
    }

    private suspend fun update(dataSource: DataSource){
        newsRepository.get(dataSource)
            .onEach {
                if (it.isEmpty()) {
                    newsMapper.map(DataState.Empty)
                } else {
                    newsMapper.map(DataState.Success(it))
                }
            }
            .catch {
                if (newsMapper.current !is DataState.Success){
                    newsMapper.map(DataState.Error(
                        Res.string.error_load_news, it
                    ))
                }
            }
            .collect()
    }
}
