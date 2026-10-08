package github.sushkpavel.studentbsuby.ui.screens.settings

import github.sushkpavel.studentbsuby.util.Event

sealed interface SettingsEvent : Event {
    class NotificationsEnabled(val enabled : Boolean) : SettingsEvent
    class CollectStatistic(val enabled: Boolean) : SettingsEvent
    class CollectCrashlytics(val enabled : Boolean) : SettingsEvent
    data object ShareLogs : SettingsEvent
    data object DontKillMyApp : SettingsEvent
}
