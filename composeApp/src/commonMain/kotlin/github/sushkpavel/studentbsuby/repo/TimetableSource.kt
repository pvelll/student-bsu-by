package github.sushkpavel.studentbsuby.repo

import github.sushkpavel.studentbsuby.data.models.Lesson
import github.sushkpavel.studentbsuby.data.models.User
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

interface TimetableSource {
    suspend fun load(owner: String, user: User?, today: LocalDate): List<List<Lesson>>
}

interface InstituteTimetableSource : TimetableSource {
    fun serves(user: User): Boolean
}

fun shownWeekStart(today: LocalDate): LocalDate {
    val monday = weekStart(today)
    return if (today.dayOfWeek == DayOfWeek.SUNDAY)
        monday.plus(7, DateTimeUnit.DAY) else monday
}

fun weekStart(date: LocalDate): LocalDate =
    date.minus(date.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)

fun Lesson.weekStart(): LocalDate? =
    date.takeIf(String::isNotEmpty)
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?.let(::weekStart)

fun Map<Int, List<Lesson>>.toWeek(): List<List<Lesson>> =
    List(if (keys.any { it > 5 }) 7 else 6) { day -> this[day].orEmpty() }
