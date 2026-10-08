package github.sushkpavel.studentbsuby.ui.screens.drawer.paidservices

import github.sushkpavel.studentbsuby.util.Event

sealed interface PaidServicesEvent : Event {
    data object UpdateRequested : PaidServicesEvent
    data object EripHelpClicked : PaidServicesEvent
}
