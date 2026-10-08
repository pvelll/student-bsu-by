package github.sushkpavel.studentbsuby.ui.screens.login

import androidx.navigation.NavController
import github.sushkpavel.studentbsuby.util.Event

sealed interface LoginEvent : Event{
    class LoginChanged(val value : String) : LoginEvent
    class PasswordChanged(val value : String) : LoginEvent
    class CaptchaChanged(val value : String) : LoginEvent
    class AutoLoginChanged(val value : Boolean) : LoginEvent
    class LoginClicked(val navController: NavController) : LoginEvent
    class InitLogin(val navController: NavController) : LoginEvent
    class UpdateClicked(val keep : Boolean = false): LoginEvent
}
