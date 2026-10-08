package github.sushkpavel.studentbsuby.ui.screens.settings

import androidx.lifecycle.ViewModel
import github.sushkpavel.studentbsuby.util.EventHandler
import github.sushkpavel.studentbsuby.util.communication.StateCommunication

class SettingsViewModel(
    val state : StateCommunication<SettingsState>,
    handler : SettingsEventHandler
) : ViewModel(), EventHandler<SettingsEvent> by handler
