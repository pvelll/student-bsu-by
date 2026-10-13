package github.sushkpavel.studentbsuby.di

import androidx.compose.ui.graphics.ImageBitmap
import github.sushkpavel.studentbsuby.repo.LoginRepository
import github.sushkpavel.studentbsuby.ui.screens.drawer.ConnectivityUi
import github.sushkpavel.studentbsuby.ui.screens.drawer.ConnectivityUiSerializer
import github.sushkpavel.studentbsuby.ui.screens.login.LoginEventHandlerImpl
import github.sushkpavel.studentbsuby.ui.screens.login.LoginViewModel
import github.sushkpavel.studentbsuby.util.DataState
import github.sushkpavel.studentbsuby.util.communication.BroadcastReceiverMapper
import github.sushkpavel.studentbsuby.util.communication.SharedFlowCommunication
import github.sushkpavel.studentbsuby.util.communication.StateFlowCommunication
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val loginModule = module {

    single {
        LoginRepository(
            api = get(),
            credentialsPreferences = get(named("CredentialsPrefs")),
            captchaRecognizer = get(),
            loginCookieManager = get(),
        )
    }

    viewModel {
        val login = StateFlowCommunication("")
        val pass = StateFlowCommunication("")
        val captcha = StateFlowCommunication("")
        val captchaImage = StateFlowCommunication<DataState<ImageBitmap>>(DataState.Empty)
        val autoLogin = StateFlowCommunication(false)
        val error = SharedFlowCommunication<String>()
        val controlsEnabled = StateFlowCommunication(true)
        val connectivityMapper = BroadcastReceiverMapper("ConnectivityUi", ConnectivityUiSerializer)

        val eventHandler = LoginEventHandlerImpl(
            dispatchers = get(),
            resourceManager = get(),
            loginRepository = get(),
            syncWorkerManager = get(),
            loginMapper = login,
            passMapper = pass,
            captchaMapper = captcha,
            captchaImageMapper = captchaImage,
            autoLoginMapper = autoLogin,
            errorMapper = error,
            controlsEnabledMapper = controlsEnabled,
            connectivityMapper = connectivityMapper
        )

        LoginViewModel(
            eventHandler = eventHandler,
            dispatchers = get(),
            errorHandler = get(),
            login = login,
            password = pass,
            captcha = captcha,
            captchaImage = captchaImage,
            autoLogin = autoLogin,
            controlsEnabled = controlsEnabled,
            error = error,
            loginRepository = get()
        )
    }
}
