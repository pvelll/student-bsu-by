package github.sushkpavel.studentbsuby.ui.screens.drawer

import androidx.compose.ui.graphics.ImageBitmap
import github.sushkpavel.studentbsuby.data.models.User
import github.sushkpavel.studentbsuby.util.DataState
import github.sushkpavel.studentbsuby.util.ErrorHandler
import github.sushkpavel.studentbsuby.util.SuspendHandlerViewModel
import github.sushkpavel.studentbsuby.util.communication.Communication
import github.sushkpavel.studentbsuby.util.communication.StateCommunication
import github.sushkpavel.studentbsuby.util.dispatchers.Dispatchers

class ProfileViewModel(
    val connectivityCommunication: Communication<ConnectivityUi>,
    val userCommunication: StateCommunication<DataState<User>>,
    val imageCommunication: StateCommunication<DataState<ImageBitmap>>,
    dispatchers: Dispatchers,
    errorHandler: ErrorHandler,
    eventHandler: ProfileEventHandler
) : SuspendHandlerViewModel<ProfileEvent>(
    dispatchers = dispatchers,
    errorHandler = errorHandler,
    suspendEventHandler = eventHandler
)
