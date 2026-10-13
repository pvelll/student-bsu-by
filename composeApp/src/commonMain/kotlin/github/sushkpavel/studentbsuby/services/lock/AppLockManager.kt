package github.sushkpavel.studentbsuby.services.lock

import com.russhwolf.settings.ObservableSettings
import github.sushkpavel.studentbsuby.repo.LoginRepository
import github.sushkpavel.studentbsuby.resources.Res
import github.sushkpavel.studentbsuby.resources.biometric_prompt_enable
import github.sushkpavel.studentbsuby.resources.biometric_prompt_subtitle
import github.sushkpavel.studentbsuby.resources.biometric_prompt_title
import github.sushkpavel.studentbsuby.util.sharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class AppLockManager(
    preferences: ObservableSettings,
    private val authenticator: BiometricAuthenticator,
    private val loginRepository: LoginRepository,
    private val timeout: Duration = 30.seconds,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    private var biometricLockEnabled by sharedPreferences(preferences, false)

    private val _enabled = MutableStateFlow(biometricLockEnabled)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _authenticating = MutableStateFlow(false)
    val authenticating: StateFlow<Boolean> = _authenticating.asStateFlow()

    var isSignedIn: Boolean = loginRepository.autoLogin && loginRepository.username.isNotBlank()

    private val _locked = MutableStateFlow(isLockRequired())
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    private var backgroundMark: TimeMark? = null

    val biometryType: BiometryType
        get() = authenticator.biometryType()

    fun onBackground() {
        if (!_authenticating.value)
            backgroundMark = timeSource.markNow()
    }

    fun onForeground() {
        val mark = backgroundMark ?: return
        backgroundMark = null
        if (mark.elapsedNow() >= timeout && isLockRequired())
            _locked.value = true
    }

    suspend fun unlock(): AuthResult {
        if (!_locked.value)
            return AuthResult.Success

        if (!authenticator.canAuthenticate()) {
            saveEnabled(false)
            _locked.value = false
            return AuthResult.Unavailable
        }

        val result = authenticate(Res.string.biometric_prompt_subtitle)
        if (result == AuthResult.Success)
            _locked.value = false
        return result
    }

    suspend fun setEnabled(enabled: Boolean): Boolean {
        if (enabled == _enabled.value)
            return true
        if (enabled && authenticate(Res.string.biometric_prompt_enable) != AuthResult.Success)
            return false
        saveEnabled(enabled)
        return true
    }

    suspend fun logout() {
        withContext(Dispatchers.Default) {
            loginRepository.logout()
        }
        isSignedIn = false
        _locked.value = false
    }

    private fun isLockRequired() = _enabled.value && isSignedIn

    private fun saveEnabled(enabled: Boolean) {
        biometricLockEnabled = enabled
        _enabled.value = enabled
    }

    private suspend fun authenticate(subtitle: StringResource): AuthResult {
        if (!_authenticating.compareAndSet(expect = false, update = true))
            return AuthResult.Cancelled
        return try {
            authenticator.authenticate(
                title = getString(Res.string.biometric_prompt_title),
                subtitle = getString(subtitle)
            )
        } finally {
            _authenticating.value = false
        }
    }
}
