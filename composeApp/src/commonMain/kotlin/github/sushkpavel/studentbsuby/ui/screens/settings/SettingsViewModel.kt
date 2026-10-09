package github.sushkpavel.studentbsuby.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import github.sushkpavel.studentbsuby.services.lock.AppLockManager
import github.sushkpavel.studentbsuby.services.lock.BiometryType
import github.sushkpavel.studentbsuby.util.EventHandler
import github.sushkpavel.studentbsuby.util.communication.StateCommunication
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    val state : StateCommunication<SettingsState>,
    handler : SettingsEventHandler,
    private val appLock: AppLockManager,
) : ViewModel(), EventHandler<SettingsEvent> by handler {

    val biometry: BiometryType = appLock.biometryType

    val biometricLock: StateFlow<Boolean> = appLock.enabled

    fun setBiometricLock(enabled: Boolean) {
        viewModelScope.launch {
            appLock.setEnabled(enabled)
        }
    }
}
