package github.sushkpavel.studentbsuby.repo

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import github.sushkpavel.studentbsuby.api.AspNetForm
import github.sushkpavel.studentbsuby.data.models.Lesson
import github.sushkpavel.studentbsuby.data.models.User
import github.sushkpavel.studentbsuby.util.exceptions.IncorrectResponseException
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber

data class IseuStudent(
    val group: String,
    val course: Int?,
    val studyForm: String?,
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

private val iseuDate = Regex("""(\d{1,2})\.(\d{1,2})\.(\d{4})""")

internal fun parseIseuDate(text: String): LocalDate? =
    iseuDate.find(text)?.destructured?.let { (day, month, year) ->
        runCatching { LocalDate(year.toInt(), month.toInt(), day.toInt()) }.getOrNull()
    }

private val latinLookalikes = mapOf(
    'A' to 'А', 'B' to 'В', 'C' to 'С', 'E' to 'Е', 'H' to 'Н', 'K' to 'К',
    'M' to 'М', 'O' to 'О', 'P' to 'Р', 'T' to 'Т', 'X' to 'Х', 'Y' to 'У'
)

internal fun normalizeGroup(name: String): String =
    name.uppercase()
        .filterNot { it.isWhitespace() }
        .map { latinLookalikes[it] ?: it }
        .joinToString("")

internal class IseuTimetablePage private constructor(private val document: Document) {

    class Option(
        val value: String,
        val text: String,
        val selected: Boolean,
    )

    val form: AspNetForm = AspNetForm.parse(document)

    val submit: Pair<String, String>? = document.selectFirst("input[type=submit][name]")
        ?.let { it.attr("name") to it.attr("value") }

    fun options(list: String): List<Option> =
        document.select("select[name=$list] option").map {
            Option(
                value = it.attr("value"),
                text = it.text().clean(),
                selected = it.hasAttr("selected"),
            )
        }

    fun selected(list: String): Option? =
        options(list).let { options -> options.firstOrNull { it.selected } ?: options.firstOrNull() }

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
            if (!page.form.isValid || page.options(FACULTY).isEmpty() || page.options(WEEK).isEmpty())
                throw IncorrectResponseException()
            return page
        }
    }
}

object IseuTimetableParser {

    private const val SEPARATOR = " \\ "

    private val lessonType = Regex("""^((?:[а-яё]+\.\s*)+)(.*)$""")

    private val time = Regex("""(\d{1,2})[:.](\d{2})\s*[-–—]\s*(\d{1,2})[:.](\d{2})""")

    private class Row(
        val date: LocalDate,
        val starts: String,
        val ends: String,
        val name: String,
        val type: String,
        val teacher: String,
        val place: String,
    )

    fun parse(html: String, owner: String, group: String, week: LocalDate): List<List<Lesson>> {
        val table = Ksoup.parse(html).getElementById("TT") ?: throw IncorrectResponseException()

        val caption = table.selectFirst("caption")?.text() ?: throw IncorrectResponseException()
        if (normalizeGroup(group) !in normalizeGroup(caption) || parseIseuDate(caption) != week)
            throw IncorrectResponseException()

        val tableRows = table.select("tr")
        if (tableRows.isEmpty())
            return emptyMap<Int, List<Lesson>>().toWeek()

        val rows = mutableListOf<Row>()
        var date: LocalDate? = null
        var days = 0
        var times = 0
        for (tr in tableRows) {
            tr.selectFirst("td.cell-date")?.let { cell ->
                date = parseIseuDate(cell.selectFirst(".date")?.text() ?: cell.text())
                if (date != null) days++
            }
            val time = tr.selectFirst("td.cell-time") ?: continue
            times++
            rows += parseRow(tr, time, date ?: continue) ?: continue
        }
        if (days == 0 || times > 0 && rows.isEmpty())
            throw IncorrectResponseException()

        return rows
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
            .toWeek()
    }

    private fun parseRow(tr: Element, cell: Element, date: LocalDate): Row? {
        val (startHour, startMinute, endHour, endMinute) =
            time.find(cell.text())?.destructured ?: return null

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
            starts = "${startHour.padStart(2, '0')}:$startMinute",
            ends = "${endHour.padStart(2, '0')}:$endMinute",
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
