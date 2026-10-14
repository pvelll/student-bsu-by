package github.sushkpavel.studentbsuby.repo

import github.sushkpavel.studentbsuby.api.IseuTimetableApi
import github.sushkpavel.studentbsuby.data.models.Lesson
import github.sushkpavel.studentbsuby.data.models.User
import github.sushkpavel.studentbsuby.repo.IseuTimetablePage.Companion.COURSE
import github.sushkpavel.studentbsuby.repo.IseuTimetablePage.Companion.FACULTY
import github.sushkpavel.studentbsuby.repo.IseuTimetablePage.Companion.GROUP
import github.sushkpavel.studentbsuby.repo.IseuTimetablePage.Companion.STUDY_FORM
import github.sushkpavel.studentbsuby.repo.IseuTimetablePage.Companion.WEEK
import github.sushkpavel.studentbsuby.util.exceptions.GroupNotFoundException
import github.sushkpavel.studentbsuby.util.exceptions.IncorrectResponseException
import kotlinx.datetime.LocalDate

class IseuTimetableSource(private val api: IseuTimetableApi) : InstituteTimetableSource {

    override fun serves(user: User): Boolean = IseuStudent.from(user) != null

    override suspend fun load(owner: String, user: User?, today: LocalDate): List<List<Lesson>> {
        val student = user?.let(IseuStudent::from)?.takeIf { it.group.isNotBlank() }
            ?: throw GroupNotFoundException()

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

                    return page.show(owner, student.group, groupOption, today)
                }
            }
        }
        throw GroupNotFoundException()
    }

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
        owner: String,
        group: String,
        groupOption: IseuTimetablePage.Option,
        today: LocalDate
    ): List<List<Lesson>> {
        val weeks = options(WEEK).mapNotNull { option -> parseIseuDate(option.text)?.let { it to option } }
        val (week, weekOption) = listOf(shownWeekStart(today), weekStart(today))
            .firstNotNullOfOrNull { monday -> weeks.firstOrNull { it.first == monday } }
            ?: return emptyMap<Int, List<Lesson>>().toWeek()

        val submit = submit ?: throw IncorrectResponseException()
        val lists = lists() + (GROUP to groupOption.value) + (WEEK to weekOption.value)
        val body = form.postback(eventTarget = "", extra = lists + submit)

        return IseuTimetableParser.parse(api.postback(body).html(), owner, group, week)
    }

    private fun IseuTimetablePage.Option.isStudyForm(studyForm: String?): Boolean =
        studyForm != null && text.lowercase().startsWith(studyForm.take(4))

    private fun IseuTimetablePage.Option.isCourse(course: Int?): Boolean =
        course != null && (value == course.toString() || text.startsWith("$course "))

    private fun abbreviation(name: String): String =
        name.split(' ').filter(String::isNotEmpty).joinToString("") { it.first().uppercase() }
}
