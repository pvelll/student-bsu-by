package github.sushkpavel.studentbsuby.repo

import github.sushkpavel.studentbsuby.api.IseuTimetableApi
import github.sushkpavel.studentbsuby.data.models.Lesson
import github.sushkpavel.studentbsuby.repo.IseuTimetablePage.Companion.COURSE
import github.sushkpavel.studentbsuby.repo.IseuTimetablePage.Companion.FACULTY
import github.sushkpavel.studentbsuby.repo.IseuTimetablePage.Companion.GROUP
import github.sushkpavel.studentbsuby.repo.IseuTimetablePage.Companion.STUDY_FORM
import github.sushkpavel.studentbsuby.repo.IseuTimetablePage.Companion.WEEK
import github.sushkpavel.studentbsuby.util.exceptions.IncorrectResponseException
import github.sushkpavel.studentbsuby.util.exceptions.IseuGroupNotFoundException
import kotlinx.datetime.LocalDate

/**
 * Loads the week timetable of an МГЭИ student from rsp.iseu.by.
 *
 * The group is looked up by name: the lists are switched faculty → form of study → course
 * (a postback per list, only when the needed value is not selected yet) until the group
 * list contains the group of the student. The faculty is guessed by its abbreviation in the
 * profile ("... ФМОС"), the form of study and the course are taken from the profile too;
 * only when a value is not recognized all options of the list are tried.
 * A first-year day student of the default faculty costs two requests: the page and "Показать".
 */
class IseuTimetableLoader(private val api: IseuTimetableApi) {

    suspend fun load(owner: String, student: IseuStudent, today: LocalDate): List<List<Lesson>> {
        if (student.group.isBlank())
            throw IseuGroupNotFoundException()

        val group = normalizeGroup(student.group)
        var page = IseuTimetablePage.parse(api.page().html())

        for (faculty in page.candidates(FACULTY) { abbreviation(it.text) in student.abbreviations }) {
            page = page.choose(FACULTY, faculty)

            for (studyForm in page.candidates(STUDY_FORM) { it.isStudyForm(student.studyForm) }) {
                page = page.choose(STUDY_FORM, studyForm)

                for (course in page.candidates(COURSE) { it.isCourse(student.course) }) {
                    page = page.choose(COURSE, course)

                    val groupOption = page.options(GROUP)
                        .firstOrNull { normalizeGroup(it.text) == group }
                        ?: continue

                    return IseuTimetableParser.parse(
                        html = page.show(groupOption, iseuWeekStart(today)),
                        owner = owner,
                        group = student.group
                    )
                }
            }
        }
        throw IseuGroupNotFoundException()
    }

    /**
     * Options of the [list] matching the profile. When nothing matches (or the profile
     * has no such information) all options are tried, the selected one first: it is
     * reached without a postback.
     */
    private fun IseuTimetablePage.candidates(
        list: String,
        matches: (IseuTimetablePage.Option) -> Boolean
    ): List<IseuTimetablePage.Option> {
        val options = options(list)
        val selected = selected(list)?.value
        return options.filter(matches)
            .ifEmpty { options.sortedByDescending { it.value == selected } }
    }

    private suspend fun IseuTimetablePage.choose(
        list: String,
        option: IseuTimetablePage.Option
    ): IseuTimetablePage {
        if (selected(list)?.value == option.value)
            return this
        val body = form.postback(eventTarget = list, extra = lists() + (list to option.value))
        return IseuTimetablePage.parse(api.postback(body).html())
    }

    private suspend fun IseuTimetablePage.show(
        group: IseuTimetablePage.Option,
        week: LocalDate
    ): String {
        val submit = submit ?: throw IncorrectResponseException()
        val weeks = options(WEEK)
        val weekOption = weeks.firstOrNull { parseIseuDate(it.text) == week }
            ?: weeks.firstOrNull { it.isCurrentWeek }

        val lists = lists() + (GROUP to group.value) +
                listOfNotNull(weekOption?.let { WEEK to it.value })
        val body = form.postback(eventTarget = "", extra = lists + submit)
        return api.postback(body).html()
    }

    // "дневная" / "дневная форма", "заочная" / "заочная форма"
    private fun IseuTimetablePage.Option.isStudyForm(studyForm: String?): Boolean =
        studyForm != null && text.lowercase().startsWith(studyForm.take(4))

    private fun IseuTimetablePage.Option.isCourse(course: Int?): Boolean =
        course != null && (value == course.toString() || text.startsWith("$course "))

    /** "Факультет мониторинга окружающей среды" -> "ФМОС" */
    private fun abbreviation(name: String): String =
        name.split(' ').filter(String::isNotEmpty).joinToString("") { it.first().uppercase() }
}
