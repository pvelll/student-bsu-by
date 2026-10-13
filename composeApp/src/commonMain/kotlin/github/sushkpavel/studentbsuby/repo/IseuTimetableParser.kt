package github.sushkpavel.studentbsuby.repo

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import github.sushkpavel.studentbsuby.api.AspNetForm
import github.sushkpavel.studentbsuby.data.models.Lesson
import github.sushkpavel.studentbsuby.data.models.User
import github.sushkpavel.studentbsuby.util.exceptions.IncorrectResponseException
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Student of МГЭИ им. А.Д. Сахарова as the profile of student.bsu.by describes them:
 * the faculty is the institute ("Учреждение образования «Международный государственный
 * экологический институт имени А.Д.Сахарова» БГУ"), the info is
 * "1 курс, группа А61ИТ2, форма обучения дневная, специальность: ... ФМОС".
 */
data class IseuStudent(
    val group: String,
    val course: Int?,
    val studyForm: String?,
    /** Upper case words of the profile info, e.g. the faculty abbreviation ("ФМОС"). */
    val abbreviations: Set<String>,
) {
    companion object {

        private val institute = listOf(
            "сахаров", "мгэи", "международный государственный экологический"
        )

        private val group = Regex("""группа\s+([^\s,;]+)""", RegexOption.IGNORE_CASE)
        private val course = Regex("""(\d+)\s*курс""", RegexOption.IGNORE_CASE)
        private val studyForm = Regex("""форма\s+обучения[\s:]+([а-яё]+)""", RegexOption.IGNORE_CASE)
        private val separators = Regex("""[\s,.;:()«»"]+""")

        /** Null when the student does not study at МГЭИ. */
        fun from(user: User): IseuStudent? {
            if (institute.none { user.faculty.contains(it, ignoreCase = true) })
                return null
            val info = user.info
            return IseuStudent(
                group = group.find(info)?.groupValues?.get(1).orEmpty(),
                course = course.find(info)?.groupValues?.get(1)?.toIntOrNull(),
                studyForm = studyForm.find(info)?.groupValues?.get(1)?.lowercase(),
                abbreviations = info.split(separators)
                    .filter { word -> word.length > 1 && word.all { it in 'А'..'Я' || it == 'Ё' } }
                    .toSet()
            )
        }
    }
}

/** Monday of the week that is shown on [today]. On Sunday the next week is shown. */
fun iseuWeekStart(today: LocalDate): LocalDate {
    val monday = weekStart(today)
    return if (today.dayOfWeek == DayOfWeek.SUNDAY)
        monday.plus(7, DateTimeUnit.DAY) else monday
}

/** Monday of the week of [Lesson.date], null for undated lessons. */
fun Lesson.weekStart(): LocalDate? =
    date.takeIf(String::isNotEmpty)
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?.let(::weekStart)

private fun weekStart(date: LocalDate): LocalDate =
    date.minus(date.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)

private val iseuDate = Regex("""(\d{1,2})\.(\d{1,2})\.(\d{4})""")

/** "12.10.2026" (also "12.10.2026 0:00:00") */
internal fun parseIseuDate(text: String): LocalDate? =
    iseuDate.find(text)?.destructured?.let { (day, month, year) ->
        runCatching { LocalDate(year.toInt(), month.toInt(), day.toInt()) }.getOrNull()
    }

private val latinLookalikes = mapOf(
    'A' to 'А', 'B' to 'В', 'C' to 'С', 'E' to 'Е', 'H' to 'Н', 'K' to 'К',
    'M' to 'М', 'O' to 'О', 'P' to 'Р', 'T' to 'Т', 'X' to 'Х', 'Y' to 'У'
)

/** Group names are typed by hand: the case, spaces and latin lookalikes may differ. */
internal fun normalizeGroup(name: String): String =
    name.uppercase()
        .filterNot { it.isWhitespace() }
        .map { latinLookalikes[it] ?: it }
        .joinToString("")

/**
 * Form state of the rsp.iseu.by timetable page: hidden ASP.NET fields, the lists
 * and the submit button.
 */
internal class IseuTimetablePage private constructor(private val document: Document) {

    class Option(
        val value: String,
        val text: String,
        val selected: Boolean,
        val isCurrentWeek: Boolean,
    )

    val form: AspNetForm = AspNetForm.parse(document)

    /** name to value of the "Показать" button. */
    val submit: Pair<String, String>? = document.selectFirst("input[type=submit][name]")
        ?.let { it.attr("name") to it.attr("value") }

    fun options(list: String): List<Option> =
        document.select("select[name=$list] option").map {
            Option(
                value = it.attr("value"),
                text = it.text().clean(),
                selected = it.hasAttr("selected"),
                isCurrentWeek = it.hasClass("current-week-option")
            )
        }

    /** The option the browser posts: the selected one or the first one. */
    fun selected(list: String): Option? =
        options(list).let { options -> options.firstOrNull { it.selected } ?: options.firstOrNull() }

    /** Values of all lists as the browser posts them. */
    fun lists(): Map<String, String> =
        document.select("select[name]").associate { select ->
            val name = select.attr("name")
            name to selected(name)?.value.orEmpty()
        }

    companion object {
        const val FACULTY = "ddlFac"
        const val STUDY_FORM = "ddlDep"
        const val COURSE = "ddlCourse"
        const val GROUP = "ddlGroup"
        const val WEEK = "ddlWeek"

        fun parse(html: String): IseuTimetablePage {
            val page = IseuTimetablePage(Ksoup.parse(html))
            if (!page.form.isValid || page.options(GROUP).isEmpty() && page.options(FACULTY).isEmpty())
                throw IncorrectResponseException()
            return page
        }
    }
}

/**
 * Parser of the timetable table (`table#TT`) of rsp.iseu.by. The table lists a week:
 * a day starts with a row whose first cell (`cell-date`, rowspan) holds the weekday and
 * the date, every row is one lesson:
 *
 * `cell-time` "08:30-09:50", `cell-subgroup`, `cell-discipline` "лек. Физика" with an
 * optional `span.topic` ("2п/гр", "Кураторский час"), `cell-staff` "доц. Иванов И.И.",
 * `cell-auditory` "ауд. 103, К2".
 *
 * Parallel lessons (subgroups, foreign language groups) are rows with the same time; they
 * are merged into one lesson like the parallel lessons of student.bsu.by.
 */
object IseuTimetableParser {

    private const val SEPARATOR = " \\ "

    // "лек. ", "практ. зан. ", "лаб. ": lower case abbreviations before the discipline
    private val lessonType = Regex("""^((?:[а-яё]+\.\s*)+)(.*)$""")

    private class Row(
        val date: LocalDate,
        val starts: String,
        val ends: String,
        val name: String,
        val type: String,
        val teacher: String,
        val place: String,
    )

    /**
     * @param group the timetable must belong to: the server answers with the timetable
     * of the group it ended up with, which differs from the requested one when the
     * postback went wrong.
     */
    fun parse(html: String, owner: String, group: String? = null): List<List<Lesson>> {
        val document = Ksoup.parse(html)
        val table = document.getElementById("TT") ?: throw IncorrectResponseException()

        val caption = table.selectFirst("caption")?.text().orEmpty()
        if (group != null && caption.isNotBlank() &&
            normalizeGroup(group) !in normalizeGroup(caption)
        ) throw IncorrectResponseException()

        val rows = mutableListOf<Row>()
        var date: LocalDate? = null
        for (tr in table.select("tr")) {
            tr.selectFirst("td.cell-date")?.let { cell ->
                date = parseIseuDate(cell.selectFirst(".date")?.text() ?: cell.text())
            }
            val time = tr.selectFirst("td.cell-time") ?: continue
            rows += parseRow(tr, time, date ?: continue) ?: continue
        }

        val lessons = rows
            .groupBy { Triple(it.date, it.starts, it.ends) }
            .map { (key, parallel) ->
                val (day, starts, ends) = key
                Lesson(
                    owner = owner,
                    dayOfWeek = day.dayOfWeek.isoDayNumber - 1,
                    number = 0,
                    name = parallel.join { it.name },
                    place = parallel.join { it.place },
                    type = parallel.join { it.type },
                    teacher = parallel.join { it.teacher },
                    starts = starts,
                    ends = ends,
                    date = day.toString()
                )
            }
            .groupBy { it.dayOfWeek }
            .mapValues { (_, day) ->
                day.sortedBy { it.starts }.mapIndexed { idx, it -> it.copy(number = idx + 1) }
            }

        val days = if (lessons.keys.any { it > 5 }) 7 else 6
        return List(days) { lessons[it].orEmpty() }
    }

    private fun parseRow(tr: Element, time: Element, date: LocalDate): Row? {
        val (starts, ends) = time.text().clean().split('-', '–')
            .map { it.trim().padStart(5, '0') }
            .takeIf { it.size == 2 }
            ?: return null

        val discipline = tr.selectFirst("td.cell-discipline")
        val topic = discipline?.select(".topic")?.text()?.clean().orEmpty()
        val subject = discipline?.ownText()?.clean().orEmpty()
        val subgroup = tr.selectFirst("td.cell-subgroup")?.text()?.clean().orEmpty()

        val match = lessonType.find(subject)
        val type = match?.groupValues?.get(1)?.clean().orEmpty()
        val title = match?.groupValues?.get(2)?.clean() ?: subject

        val details = listOf(subgroup, topic).filter(String::isNotEmpty)
        val name = when {
            title.isNotEmpty() && details.isNotEmpty() ->
                "$title (${details.joinToString(", ")})"
            title.isNotEmpty() -> title
            else -> details.joinToString(", ")
        }
        if (name.isEmpty())
            return null

        return Row(
            date = date,
            starts = starts,
            ends = ends,
            name = name,
            type = type,
            teacher = tr.selectFirst("td.cell-staff")?.text()?.clean().orEmpty(),
            place = tr.selectFirst("td.cell-auditory")?.text()?.clean()
                ?.removePrefix("ауд.")?.trim(' ', ',').orEmpty(),
        )
    }

    private fun List<Row>.join(value: (Row) -> String): String =
        map(value).filter(String::isNotEmpty).distinct().joinToString(SEPARATOR)
}

private val whitespace = Regex("""\s+""")

private fun String.clean(): String =
    replace(' ', ' ').replace(whitespace, " ").trim()
