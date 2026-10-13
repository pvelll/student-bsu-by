package github.sushkpavel.studentbsuby.di

import androidx.compose.ui.graphics.ImageBitmap
import github.sushkpavel.studentbsuby.data.models.User
import github.sushkpavel.studentbsuby.repo.PhotoRepository
import github.sushkpavel.studentbsuby.repo.UserRepository
import github.sushkpavel.studentbsuby.ui.screens.drawer.ConnectivityUi
import github.sushkpavel.studentbsuby.ui.screens.drawer.ConnectivityUiSerializer
import github.sushkpavel.studentbsuby.ui.screens.drawer.DrawerRoute
import github.sushkpavel.studentbsuby.ui.screens.drawer.ProfileEventHandlerImpl
import github.sushkpavel.studentbsuby.ui.screens.drawer.ProfileViewModel
import github.sushkpavel.studentbsuby.util.DataState
import github.sushkpavel.studentbsuby.util.communication.BroadcastReceiverCommunication
import github.sushkpavel.studentbsuby.util.communication.BroadcastReceiverMapper
import github.sushkpavel.studentbsuby.util.communication.StateFlowCommunication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val profileModule = module {

    factory { UserRepository(get(), get(), get()) }

    factory { PhotoRepository(get(), get()) }

    viewModel {
        val imageCommunication = StateFlowCommunication<DataState<ImageBitmap>>(DataState.Loading)
        val userCommunication = StateFlowCommunication<DataState<User>>(DataState.Loading)
        val routeCommunication = StateFlowCommunication<DrawerRoute>(DrawerRoute.Timetable)
        val connectivityCommunication = BroadcastReceiverCommunication("ConnectivityUi", ConnectivityUiSerializer)
        val connectivityMapper = BroadcastReceiverMapper("ConnectivityUi", ConnectivityUiSerializer)

        val eventHandler = ProfileEventHandlerImpl(
            dispatchers = get(),
            connectivityManager = get(),
            loginRepository = get(),
            userRepository = get(),
            photoRepository = get(),
            routeMapper = routeCommunication,
            connectivityMapper = connectivityMapper,
            imageMapper = imageCommunication,
            userMapper = userCommunication
        )

        ProfileViewModel(
            connectivityCommunication = connectivityCommunication,
            userCommunication = userCommunication,
            imageCommunication = imageCommunication,
            dispatchers = get(),
            errorHandler = get(),
            eventHandler = eventHandler
        )
    }
}
