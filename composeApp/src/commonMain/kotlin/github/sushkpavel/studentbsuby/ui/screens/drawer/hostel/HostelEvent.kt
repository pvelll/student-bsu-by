package github.sushkpavel.studentbsuby.ui.screens.drawer.hostel

import github.sushkpavel.studentbsuby.data.models.HostelAdvert
import github.sushkpavel.studentbsuby.repo.HostelState
import github.sushkpavel.studentbsuby.util.Event

sealed interface HostelEvent : Event {
    data object UpdateRequested : HostelEvent
    class ShowHostelOnMapClicked(val hostel: HostelState.Provided) : HostelEvent
    class ShowAdOnMapClicked(val ad : HostelAdvert) : HostelEvent
    class CallClicked(val ad : HostelAdvert) : HostelEvent
}
