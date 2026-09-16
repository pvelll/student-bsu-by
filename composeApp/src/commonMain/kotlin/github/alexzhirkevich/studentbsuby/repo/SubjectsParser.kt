package github.alexzhirkevich.studentbsuby.repo

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import github.alexzhirkevich.studentbsuby.api.asyncPostbackHtml
import github.alexzhirkevich.studentbsuby.data.models.Subject

/**
 * Parser of the "Успеваемость" (StudProgress) page of student.bsu.by.
 *
 * The marks table (`tblProgress`) has 10 columns: number, subject, 6 hour columns,
 * credit ("Зачет") and exam ("Экзамен"). When all sessions are requested the table
 * contains one block per session, each starting with a caption row
 * (`<td colspan="10"><b>1 курс, зимняя сессия</b></td>`) followed by two header rows.
 * A credit/exam that has not been taken yet is rendered in italics ("<i>зачет</i>"),
 * unsatisfactory marks in red, retakes are marked with apostrophes.
 */
object SubjectsParser {

    private val sessionCaption = Regex("""\d+\s*курс""", RegexOption.IGNORE_CASE)

    private const val COLUMNS = 10

    fun parse(html: String, owner: String): List<List<Subject>> {
        val document = Ksoup.parse(html.asyncPostbackHtml())

        val rows = document.select("table[id*=tblProgress] tr")
            .ifEmpty { document.getElementsByTag("tr") }

        val semesters = mutableListOf<MutableList<Subject>>()
        var hasCaptions = false
        var previousNumber = Int.MIN_VALUE

        for (row in rows) {
            val cells = row.children().filter {
                it.tagName().equals("td", ignoreCase = true) ||
                        it.tagName().equals("th", ignoreCase = true)
            }
            if (cells.isEmpty())
                continue

            if (cells.isSessionCaption()) {
                hasCaptions = true
                semesters.add(mutableListOf())
                previousNumber = Int.MIN_VALUE
                continue
            }

            val number = cells[0].cellText().toIntOrNull() ?: continue
            if (cells.size < COLUMNS)
                continue

            // Pages without caption rows: numbering restarts for every session.
            if (semesters.isEmpty() || (!hasCaptions && number <= previousNumber)) {
                semesters.add(mutableListOf())
            }
            previousNumber = number

            semesters.last().add(parseSubject(cells, owner, semesters.size))
        }

        return semesters
    }

    /**
     * Zero based index of the session selected on the page (bold link in the session
     * selector), or null when "Все сессии" is selected / the selector is missing.
     */
    fun currentSemesterIndex(html: String): Int? {
        val document = Ksoup.parse(html.asyncPostbackHtml())
        val selected = document.select("a[id*=selSemester]").firstOrNull {
            it.attr("style").replace(" ", "").contains("font-weight:bold", ignoreCase = true) ||
                    it.selectFirst("b") != null
        } ?: return null

        return selected.id()
            .substringAfterLast("selSemester")
            .toIntOrNull()
            ?.let { it - 1 }
            ?.takeIf { it >= 0 }
    }

    private fun List<Element>.isSessionCaption(): Boolean {
        if (size != 1)
            return false
        val colspan = first().attr("colspan").toIntOrNull() ?: 1
        return colspan > 1 && sessionCaption.containsMatchIn(first().cellText())
    }

    private fun parseSubject(cells: List<Element>, owner: String, semester: Int): Subject {
        val hours = (2..7).map { cells[it].cellText().toIntOrNull() ?: 0 }
        val credit = MarkCell.from(cells[8])
        val exam = MarkCell.from(cells[9])

        return Subject(
            semester = semester,
            owner = owner,
            name = cells[1].cellText(),
            lectures = hours[0],
            practice = hours[1],
            labs = hours[2],
            seminars = hours[3],
            facults = hours[4],
            ksr = hours[5],
            hasCredit = credit != null,
            creditPassed = credit?.passed,
            creditMark = credit?.mark,
            creditRetakes = credit?.retakes ?: 0,
            hasExam = exam != null,
            examMark = exam?.mark,
            examRetakes = exam?.retakes ?: 0
        )
    }

    private fun Element.cellText(): String =
        text().replace(' ', ' ').trim()

    private class MarkCell(
        val text: String,
        val pending: Boolean,
        val failed: Boolean,
    ) {
        val retakes: Int = text.count { it == '\'' || it == '’' || it == '`' }

        val mark: Int? = text.filter(Char::isDigit).toIntOrNull()

        val passed: Boolean? = when {
            text.contains('+') -> true
            text.contains('-') -> false
            failed -> false
            mark != null -> mark >= 4
            pending -> null
            text.contains("зач", ignoreCase = true) ->
                !(text.contains("не", ignoreCase = true) || text.contains("не", ignoreCase = true))
            else -> null
        }

        companion object {
            fun from(cell: Element): MarkCell? {
                val text = cell.cellText()
                if (text.isEmpty())
                    return null
                val pending = cell.selectFirst("i, em") != null
                val failed = cell.select("[color], [style*=color]").any {
                    it.attr("color").contains("red", ignoreCase = true) ||
                            it.attr("style").contains("red", ignoreCase = true)
                }
                return MarkCell(text, pending, failed)
            }
        }
    }
}
