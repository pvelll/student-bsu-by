package github.sushkpavel.studentbsuby.ui.screens.drawer.about

import github.sushkpavel.studentbsuby.util.Event

sealed interface AboutEvent : Event {
    data object EmailClicked : AboutEvent
    data object TgClicked : AboutEvent
}
