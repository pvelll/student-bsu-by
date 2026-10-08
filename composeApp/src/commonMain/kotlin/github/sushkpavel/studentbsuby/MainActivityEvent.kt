package github.sushkpavel.studentbsuby

import github.sushkpavel.studentbsuby.util.Event

sealed interface MainActivityEvent : Event {
    object Initialized : MainActivityEvent

    object ExitClicked : MainActivityEvent
    object UpdateClicked : MainActivityEvent
}
