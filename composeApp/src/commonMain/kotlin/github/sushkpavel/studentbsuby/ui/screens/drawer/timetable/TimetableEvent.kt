package github.sushkpavel.studentbsuby.ui.screens.drawer.timetable

import github.sushkpavel.studentbsuby.data.models.Lesson
import github.sushkpavel.studentbsuby.util.Event

enum class LessonState{
    INCOMING,
    RUNNING,
    PASSED
}

typealias Timetable = List<List<Pair<Lesson,LessonState>>>

sealed interface TimetableEvent : Event {
    object UpdateRequested : TimetableEvent
}
