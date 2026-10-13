package github.sushkpavel.studentbsuby.ui.screens.drawer.timetable

import github.sushkpavel.studentbsuby.data.models.Lesson
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber

fun List<List<Lesson>>.withStates(today: LocalDate, time: String): Timetable {
    val date = today.toString()
    val weekday = today.dayOfWeek.isoDayNumber - 1
    return map { day ->
        var hasRunning = false
        day.map { lesson ->
            val dayOrder = if (lesson.date.isNotEmpty())
                lesson.date.compareTo(date)
            else lesson.dayOfWeek.compareTo(weekday)

            lesson to when {
                dayOrder < 0 || dayOrder == 0 && lesson.ends <= time ->
                    LessonState.PASSED
                !hasRunning && dayOrder == 0 && lesson.ends > time ->
                    LessonState.RUNNING.also { hasRunning = true }
                else -> LessonState.INCOMING
            }
        }
    }
}
