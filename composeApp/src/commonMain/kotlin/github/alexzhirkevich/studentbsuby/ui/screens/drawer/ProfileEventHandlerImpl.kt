package github.alexzhirkevich.studentbsuby.ui.screens.drawer

import androidx.compose.ui.graphics.ImageBitmap
import androidx.navigation.NavGraph.Companion.findStartDestination
import github.alexzhirkevich.studentbsuby.resources.Res
import github.alexzhirkevich.studentbsuby.resources.error_load_photo
import github.alexzhirkevich.studentbsuby.resources.error_load_user
import github.alexzhirkevich.studentbsuby.data.models.User
import github.alexzhirkevich.studentbsuby.navigation.Route
import github.alexzhirkevich.studentbsuby.repo.DataSource
import github.alexzhirkevich.studentbsuby.repo.LoginRepository
import github.alexzhirkevich.studentbsuby.repo.PhotoRepository
import github.alexzhirkevich.studentbsuby.repo.UserRepository
import github.alexzhirkevich.studentbsuby.util.BaseSuspendEventHandler
import github.alexzhirkevich.studentbsuby.util.ConnectivityManager
import github.alexzhirkevich.studentbsuby.util.DataState
import github.alexzhirkevich.studentbsuby.util.SuspendEventHandler
import github.alexzhirkevich.studentbsuby.util.communication.Mapper
import github.alexzhirkevich.studentbsuby.util.communication.StateMapper
import github.alexzhirkevich.studentbsuby.util.dispatchers.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

interface ProfileEventHandler : SuspendEventHandler<ProfileEvent>

class ProfileEventHandlerImpl(
    private val dispatchers: Dispatchers,
    private val connectivityManager: ConnectivityManager,
    private val loginRepository: LoginRepository,
    private val userRepository: UserRepository,
    private val photoRepository: PhotoRepository,
    private val routeMapper: StateMapper<DrawerRoute>,
    private val connectivityMapper: Mapper<ConnectivityUi>,
    private val imageMapper: StateMapper<DataState<ImageBitmap>>,
    private val userMapper: StateMapper<DataState<User>>
) : ProfileEventHandler, SuspendEventHandler<ProfileEvent> by SuspendEventHandler.from(
    LogoutEventHandler(dispatchers, loginRepository),
    RouteSelectedHandler(routeMapper, dispatchers),
    SettingClickedHandler(dispatchers),
    UpdateRequestedHandler(
        connectivityManager = connectivityManager,
        userRepository = userRepository,
        photoRepository = photoRepository,
        loginRepository = loginRepository,
        userMapper = userMapper,
        imageMapper = imageMapper,
        connectivityMapper = connectivityMapper,
    )
)

private class RouteSelectedHandler(
    private val routeMapper : StateMapper<DrawerRoute>,
    private val dispatchers: Dispatchers
) : BaseSuspendEventHandler<ProfileEvent.RouteSelected>(
    ProfileEvent.RouteSelected::class
) {
    override suspend fun handle(event: ProfileEvent.RouteSelected) {
        dispatchers.runOnUI {
            if (routeMapper.current.route != event.route.route) {
                routeMapper.map(event.route)
                event.navController.navigate(event.route.route.route) {
                    val last = event.navController.currentDestination?.id
                            ?: event.navController.graph.findStartDestination().id
                    popUpTo(last) {
                        saveState = true
                        inclusive = true
                    }

                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }
}

private class SettingClickedHandler(
    private val dispatchers: Dispatchers
) : BaseSuspendEventHandler<ProfileEvent.SettingsClicked>(
    ProfileEvent.SettingsClicked::class
) {
    override suspend fun handle(event: ProfileEvent.SettingsClicked) {
        // Events are handled on a background dispatcher; NavController must be
        // driven from the main thread, otherwise the navigation silently fails.
        dispatchers.runOnUI {
            event.navController.navigate(Route.SettingsScreen.route) {
                launchSingleTop = true
            }
        }
    }
}

private class LogoutEventHandler(
    private val dispatchers: Dispatchers,
    private val loginRepository: LoginRepository,
) : BaseSuspendEventHandler<ProfileEvent.Logout>(
    ProfileEvent.Logout::class
) {
    override suspend fun handle(event: ProfileEvent.Logout) {
       loginRepository.logout()
        dispatchers.runOnUI {
            event.navController.popBackStack()
            event.navController.navigate(Route.AuthScreen.route)
        }
    }
}

private class UpdateRequestedHandler(
    private val connectivityManager: ConnectivityManager,
    private val userRepository: UserRepository,
    private val photoRepository: PhotoRepository,
    private val loginRepository: LoginRepository,
    private val userMapper : StateMapper<DataState<User>>,
    private val imageMapper : StateMapper<DataState<ImageBitmap>>,
    private val connectivityMapper: Mapper<ConnectivityUi>,
) : BaseSuspendEventHandler<ProfileEvent.UpdateRequested>(
    ProfileEvent.UpdateRequested::class
){

    override suspend fun launch() {
        userMapper.map(DataState.Loading)
        imageMapper.map(DataState.Loading)
        update(DataSource.All)
        connectivityMapper.map(ConnectivityUi.Connected)

        // The first value is the current connectivity (hot state flow), the data has
        // just been loaded above; only real changes re-check the session and reload.
        var first = true

        connectivityManager.isNetworkConnected.collect { connected ->

            val logged = if (first) true else
                runCatching {
                    loginRepository.initialize().loggedIn
                }.getOrDefault(false)

            connectivityMapper.map(when{
                !connected -> ConnectivityUi.Connecting
                !logged -> ConnectivityUi.Offline
                else -> ConnectivityUi.Connected
            })

            if (connected && !first){
                update(DataSource.Remote)
            }
            first = false
        }
    }
    override suspend fun handle(event: ProfileEvent.UpdateRequested) {
        update(DataSource.Remote)
    }

    private suspend fun update(source: DataSource) = coroutineScope {
        launch {
            photoRepository.get(source)
                .onEach {
                    imageMapper.map(DataState.Success(it))
                }
                .onEmpty {
                    imageMapper.map(DataState.Empty)
                }
                .catch {
                    if (imageMapper.current !is DataState.Success) {
                        imageMapper.map(
                            DataState.Error(
                                Res.string.error_load_photo, it
                            )
                        )
                    }
                }.collect()
        }
        launch {
            userRepository.get(source)
                .onEach {
                    userMapper.map(DataState.Success(it))
                }
                .onEmpty {
                    userMapper.map(DataState.Empty)
                }
                .catch {
                    if (userMapper.current !is DataState.Success) {
                        userMapper.map(
                            DataState.Error(
                                Res.string.error_load_user, it
                            )
                        )
                    }
                }.collect()
        }
    }
}
