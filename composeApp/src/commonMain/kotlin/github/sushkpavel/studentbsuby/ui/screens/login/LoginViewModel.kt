package github.sushkpavel.studentbsuby.ui.screens.login

import androidx.compose.ui.graphics.ImageBitmap
import github.sushkpavel.studentbsuby.util.dispatchers.Dispatchers
import github.sushkpavel.studentbsuby.repo.LoginRepository
import github.sushkpavel.studentbsuby.util.DataState
import github.sushkpavel.studentbsuby.util.ErrorHandler
import github.sushkpavel.studentbsuby.util.SuspendHandlerViewModel
import github.sushkpavel.studentbsuby.util.communication.Communication
import github.sushkpavel.studentbsuby.util.communication.StateCommunication

class LoginViewModel(
    eventHandler: LoginEventHandler,
    dispatchers: Dispatchers,
    errorHandler: ErrorHandler,
    val login : StateCommunication<String>,
    val password : StateCommunication<String>,
    val captcha : StateCommunication<String>,
    val captchaImage : StateCommunication<DataState<ImageBitmap>>,
    val autoLogin : StateCommunication<Boolean>,
    val controlsEnabled: StateCommunication<Boolean>,
    val error : Communication<String>,
    loginRepository: LoginRepository
    ) : SuspendHandlerViewModel<LoginEvent>(
    dispatchers = dispatchers,
    suspendEventHandler = eventHandler,
    errorHandler = errorHandler
) {
    // A session can only be restored when the credentials are known: the username is
    // also the key of every cache, so a "logged in" state without it is useless.
    val skipLogin = loginRepository.autoLogin && loginRepository.username.isNotBlank()
}
