package github.sushkpavel.studentbsuby.ui.screens.login

import androidx.compose.ui.graphics.ImageBitmap
import androidx.navigation.NavController
import github.sushkpavel.studentbsuby.util.dispatchers.Dispatchers
import github.sushkpavel.studentbsuby.navigation.Route
import github.sushkpavel.studentbsuby.navigation.navigate
import github.sushkpavel.studentbsuby.repo.LoginRepository
import github.sushkpavel.studentbsuby.resources.Res
import github.sushkpavel.studentbsuby.resources.error_connection
import github.sushkpavel.studentbsuby.ui.screens.drawer.ConnectivityUi
import github.sushkpavel.studentbsuby.util.*
import github.sushkpavel.studentbsuby.util.communication.BroadcastMapper
import github.sushkpavel.studentbsuby.util.communication.Mapper
import github.sushkpavel.studentbsuby.util.communication.StateMapper
import kotlinx.coroutines.*

interface LoginEventHandler : SuspendEventHandler<LoginEvent>

class LoginEventHandlerImpl(
    private val dispatchers: Dispatchers,
    private val resourceManager : ResourceManager,
    private val loginRepository: LoginRepository,
    private val syncWorkerManager: WorkerManager,
    private val loginMapper : StateMapper<String>,
    private val passMapper : StateMapper<String>,
    private val captchaMapper : StateMapper<String>,
    private val captchaImageMapper : Mapper<DataState<ImageBitmap>>,
    private val autoLoginMapper : StateMapper<Boolean>,
    private val errorMapper : Mapper<String>,
    private val controlsEnabledMapper : Mapper<Boolean>,
    private val connectivityMapper: BroadcastMapper<ConnectivityUi>,
) : LoginEventHandler, SuspendEventHandler<LoginEvent> by SuspendEventHandler.from(
    AutoLoginChangedHandler(autoLoginMapper),
    CaptchaChangedHandler(captchaMapper),
    LoginChangedHandler(loginMapper),
    InitLoginHandler(
        dispatchers =  dispatchers,
        resourceManager = resourceManager,
        connectivityMapper = connectivityMapper,
        loginRepository = loginRepository,
        syncWorkerManager = syncWorkerManager,
    ),
    LoginClickedHandler(
        dispatchers = dispatchers,
        resourceManager = resourceManager,
        loginRepository = loginRepository,
        syncWorkerManager = syncWorkerManager,
        loginMapper = loginMapper,
        passwordMapper = passMapper,
        captchaMapper = captchaMapper,
        errorMapper = errorMapper,
        autoLoginMapper = autoLoginMapper,
        controlsEnabledMapper = controlsEnabledMapper,
        updateHandler = UpdateClickedHandler(
            resourceManager = resourceManager,
            loginRepository = loginRepository,
            captchaMapper = captchaMapper,
            captchaImageMapper = captchaImageMapper,
            errorMapper = errorMapper
        )
    ),
    PasswordChangedHandler(passMapper),
    UpdateClickedHandler(
        resourceManager = resourceManager,
        loginRepository = loginRepository,
        captchaMapper = captchaMapper,
        captchaImageMapper = captchaImageMapper,
        errorMapper = errorMapper
    )
)

private class LoginChangedHandler(
    private val loginMapper: Mapper<String>
) : BaseSuspendEventHandler<LoginEvent.LoginChanged>(
    LoginEvent.LoginChanged::class
){
    override suspend fun handle(event: LoginEvent.LoginChanged) {
        loginMapper.map(event.value.replace("\n",""))
    }
}

private class PasswordChangedHandler(
    private val passwordMapper: Mapper<String>
) : BaseSuspendEventHandler<LoginEvent.PasswordChanged>(
    LoginEvent.PasswordChanged::class
){
    override suspend fun handle(event: LoginEvent.PasswordChanged) {
        passwordMapper.map(event.value.replace("\n",""))
    }
}

private class CaptchaChangedHandler(
    private val captchaMapper: Mapper<String>
) : BaseSuspendEventHandler<LoginEvent.CaptchaChanged>(
    LoginEvent.CaptchaChanged::class
){
    override suspend fun handle(event: LoginEvent.CaptchaChanged) {
        captchaMapper.map(event.value.replace("\n",""))
    }
}

private class AutoLoginChangedHandler(
    private val captchaMapper: Mapper<Boolean>
) : BaseSuspendEventHandler<LoginEvent.AutoLoginChanged>(
    LoginEvent.AutoLoginChanged::class
){
    override suspend fun handle(event: LoginEvent.AutoLoginChanged) {
        captchaMapper.map(event.value)
    }
}


private class InitLoginHandler(
    private val dispatchers: Dispatchers,
    private val resourceManager: ResourceManager,
    private val connectivityMapper : BroadcastMapper<ConnectivityUi>,
    private val loginRepository: LoginRepository,
    private val syncWorkerManager: WorkerManager,
) : BaseSuspendEventHandler<LoginEvent.InitLogin>(
    LoginEvent.InitLogin::class
) {

    override suspend fun launch() {
        kotlin.runCatching {
            syncWorkerManager.stop()
        }
    }

    override suspend fun handle(event: LoginEvent.InitLogin) {
        if (loginRepository.autoLogin && loginRepository.username.isNotBlank()) {
//            while (true) {
                try {
                    if (!loginRepository.autoLogin)
                        return

                    val init = loginRepository.initialize()

                    if (init.loggedIn) {
                        connectivityMapper.map(ConnectivityUi.Connected)
                        return navigate(dispatchers, event.navController)
                    }

                    val captcha = loginRepository.updateCaptcha(true)
                        ?: throw Exception()

                    if (!init.success) {
                        throw Exception()
                    }

                    val captchaText = kotlin.runCatching {
                        loginRepository.getCaptchaText(captcha)
                    }.getOrNull() ?: return

                    if (login(
                            dispatchers, resourceManager, loginRepository, event.navController,
                            loginRepository.username, loginRepository.password, captchaText
                        ).first
                    ) {
                        connectivityMapper.map(ConnectivityUi.Connected)
//                        kotlin.runCatching {
//                            if (!syncWorkerManager.isEnabled()) {
//                                syncWorkerManager.run()
//                            }
//                        }
                        return
                    } else {
                        connectivityMapper.map(ConnectivityUi.Offline)
                    }
                } catch (t: Throwable) {
                    connectivityMapper.map(ConnectivityUi.Connecting)
                    delay(3000)
                }
            }
//        }
    }

    companion object {
        private suspend fun navigate(dispatchers: Dispatchers, navController: NavController) {
            dispatchers.runOnUI {
                val shown = navController.currentBackStack.value.any {
                    it.destination.route == Route.DrawerScreen.route
                }
                if (shown)
                    return@runOnUI

                navController.navigate(Route.DrawerScreen.route) {
                    launchSingleTop = true
                    popUpTo(Route.AuthScreen.route) {
                        inclusive = true
                        saveState = false
                    }
                }
            }
        }

        suspend fun login(
            dispatchers: Dispatchers,
            resourceManager: ResourceManager,
            loginRepository: LoginRepository,
            navController: NavController,
            login: String,
            passsword: String,
            captcha: String
        ): Pair<Boolean, String> = try {
            val res = loginRepository.login(
                login,
                passsword,
                captcha
            )
            if (res.loggedIn) {
                navigate(dispatchers, navController)
            }
            res.loggedIn to res.loginResult.orEmpty()
        } catch (t: Throwable) {
            println("Login error: ${t.message}")
            t.printStackTrace()
            false to resourceManager.getString(Res.string.error_connection)
        }
    }
}

private class LoginClickedHandler(
    private val dispatchers: Dispatchers,
    private val resourceManager: ResourceManager,
    private val loginRepository: LoginRepository,
    private val syncWorkerManager: WorkerManager,
    private val loginMapper: StateMapper<String>,
    private val passwordMapper: StateMapper<String>,
    private val captchaMapper: StateMapper<String>,
    private val errorMapper: Mapper<String>,
    private val autoLoginMapper: StateMapper<Boolean>,
    private val controlsEnabledMapper: Mapper<Boolean>,
    private val updateHandler: SuspendEventHandler<LoginEvent.UpdateClicked>
) : BaseSuspendEventHandler<LoginEvent.LoginClicked>(
    LoginEvent.LoginClicked::class
) {
    override suspend fun launch() {
        loginMapper.map(loginRepository.username)
        passwordMapper.map(loginRepository.password)
        autoLoginMapper.map(loginRepository.autoLogin)
        // The captcha is requested by the login screen when it is shown: this view model
        // is also created by the main screen for the session restore, and downloading
        // and recognizing captchas there on every start is wasted work.
    }

    override suspend fun handle(event: LoginEvent.LoginClicked) {
        controlsEnabledMapper.map(false)
        val logged = InitLoginHandler.login(
            dispatchers = dispatchers,
            resourceManager = resourceManager,
            loginRepository = loginRepository,
            navController = event.navController,
            login = loginMapper.current,
            passsword = passwordMapper.current,
            captcha = captchaMapper.current
        )
        kotlin.runCatching {
            if (logged.first) {
                loginRepository.autoLogin = autoLoginMapper.current
//                if (!syncWorkerManager.isEnabled())
//                    syncWorkerManager.run()
            } else {
                updateHandler.handle(LoginEvent.UpdateClicked(false))
                errorMapper.map(logged.second)
            }
        }
        controlsEnabledMapper.map(true)
    }
}

private class UpdateClickedHandler(
    private val resourceManager: ResourceManager,
    private val loginRepository: LoginRepository,
    private val captchaImageMapper: Mapper<DataState<ImageBitmap>>,
    private val captchaMapper: Mapper<String>,
    private val errorMapper: Mapper<String>
) : BaseSuspendEventHandler<LoginEvent.UpdateClicked>(
    LoginEvent.UpdateClicked::class
){

    override suspend fun handle(event: LoginEvent.UpdateClicked) {
        captchaImageMapper.map(DataState.Loading)
        val image = kotlin.runCatching {
            loginRepository.updateCaptcha(event.keep)
        }.getOrNull()
        if (image == null){
            errorMapper.map(resourceManager.getString(Res.string.error_connection))
            captchaImageMapper.map(DataState.Error(Res.string.error_connection))
        } else {
            captchaImageMapper.map(DataState.Success(image))
            captchaMapper.map(loginRepository.getCaptchaText(image))
        }
    }
}
