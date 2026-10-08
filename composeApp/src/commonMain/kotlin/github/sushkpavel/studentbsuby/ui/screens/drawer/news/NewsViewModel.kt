package github.sushkpavel.studentbsuby.ui.screens.drawer.news

import github.sushkpavel.studentbsuby.data.models.News
import github.sushkpavel.studentbsuby.util.DataState
import github.sushkpavel.studentbsuby.util.ErrorHandler
import github.sushkpavel.studentbsuby.util.SuspendEventHandler
import github.sushkpavel.studentbsuby.util.SuspendHandlerViewModel
import github.sushkpavel.studentbsuby.util.Updatable
import github.sushkpavel.studentbsuby.util.communication.StateCommunication
import github.sushkpavel.studentbsuby.util.dispatchers.Dispatchers

interface NewsEventHandler : SuspendEventHandler<NewsEvent>

class NewsViewModel(
    override val isUpdating: StateCommunication<Boolean>,
    val newsCommunication: StateCommunication<DataState<List<News>>>,
    errorHandler: ErrorHandler,
    dispatchers: Dispatchers,
    eventHandler: NewsEventHandler
) : SuspendHandlerViewModel<NewsEvent>(
    errorHandler = errorHandler,
    dispatchers = dispatchers,
    suspendEventHandler = eventHandler
), Updatable {

    override fun update() {
        handle(NewsEvent.UpdateRequested)
    }
}
