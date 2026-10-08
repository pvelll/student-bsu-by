package github.sushkpavel.studentbsuby.ui.screens.drawer.news

import github.sushkpavel.studentbsuby.util.Event

sealed interface NewsEvent : Event {
    data object UpdateRequested : NewsEvent
}
