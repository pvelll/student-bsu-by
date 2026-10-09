package github.sushkpavel.studentbsuby.ui.screens.drawer.progress

import github.sushkpavel.studentbsuby.util.Event

sealed interface ProgressEvent : Event {
    object UpdateRequested : ProgressEvent
}
