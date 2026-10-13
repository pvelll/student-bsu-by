package github.sushkpavel.studentbsuby.ui.screens.drawer.progress

import github.sushkpavel.studentbsuby.data.models.Subject
import kotlin.math.ceil
import kotlin.math.roundToInt

val MarkRange = 1..10

const val PassingMark = 4

enum class MarkKind { Exam, Credit }

data class Mark(
    val subject: String,
    val semester: Int,
    val kind: MarkKind,
    val value: Int,
    val retakes: Int,
)

data class SemesterAverage(
    val semester: Int,
    val average: Double,
    val cumulative: Double,
    val marks: Int,
)

data class RatedSubject(
    val name: String,
    val semester: Int,
    val mark: Int,
    val kind: MarkKind,
)

data class SessionProgress(
    val semester: Int,
    val examsTotal: Int,
    val examsPassed: Int,
    val creditsTotal: Int,
    val creditsPassed: Int,
    val pending: List<String>,
) {
    val total get() = examsTotal + creditsTotal
    val passed get() = examsPassed + creditsPassed
    val isFinished get() = passed == total
}

data class AcademicProgress(
    val marks: List<Mark>,
    val semesters: List<SemesterAverage>,
    val semesterCount: Int,
    val currentSemester: Int,
    val session: SessionProgress?,
    val totalRetakes: Int,
    val upcomingExams: Int,
) {
    val average: Double? = marks.averageOrNull()

    val sum: Int get() = marks.sumOf { it.value }

    val hasMarks: Boolean get() = marks.isNotEmpty()

    val lastDelta: Double?
        get() = if (semesters.size < 2) null
        else semesters[semesters.lastIndex].average - semesters[semesters.lastIndex - 1].average

    val bestSemester: SemesterAverage?
        get() = semesters.maxByOrNull { it.average }

    val excellentMarks: Int get() = marks.count { it.value == MarkRange.last }

    val ratedSemesters: List<Int> get() = semesters.map { it.semester }

    fun marksOf(semester: Int?): List<Mark> =
        if (semester == null) marks else marks.filter { it.semester == semester }

    fun distribution(semester: Int? = null): List<Int> {
        val counts = IntArray(MarkRange.last)
        marksOf(semester).forEach { counts[it.value - 1]++ }
        return counts.toList()
    }

    fun rating(semester: Int? = null): List<RatedSubject> =
        marksOf(semester)
            .groupBy { it.subject to it.semester }
            .map { (key, marks) ->
                val mark = marks.firstOrNull { it.kind == MarkKind.Exam } ?: marks.first()
                RatedSubject(key.first, key.second, mark.value, mark.kind)
            }
            .sortedWith(
                compareByDescending<RatedSubject> { it.mark }
                    .thenByDescending { it.semester }
                    .thenBy { it.name }
            )

    fun bestPossible(count: Int): Double =
        (sum + MarkRange.last * count).toDouble() / (marks.size + count)

    fun goal(target: Double, count: Int): Goal {
        require(count > 0)
        val total = marks.size + count
        val needed = target * total - sum
        val required = needed / count
        val best = bestPossible(count)

        return when {
            average != null && average >= target && required <= PassingMark ->
                Goal.AlreadyReached(required.coerceAtLeast(0.0))
            required <= PassingMark -> Goal.AnyPassingMarks
            required > MarkRange.last -> Goal.Unreachable(best)
            else -> Goal.Reachable(required, exampleMarks(needed, count))
        }
    }

    sealed interface Goal {
        data class AlreadyReached(val required: Double) : Goal
        data object AnyPassingMarks : Goal
        data class Reachable(val required: Double, val example: List<Int>) : Goal
        data class Unreachable(val best: Double) : Goal
    }

    companion object {

        fun from(subjects: List<List<Subject>>, currentSemester: Int?): AcademicProgress {
            val current = (currentSemester ?: subjects.lastIndex)
                .coerceIn(0, (subjects.size - 1).coerceAtLeast(0))

            val marks = subjects.flatMapIndexed { semester, list ->
                list.flatMap { it.marks(semester) }
            }

            var sum = 0
            var count = 0
            val semesters = marks
                .groupBy { it.semester }
                .entries
                .sortedBy { it.key }
                .map { (semester, list) ->
                    sum += list.sumOf { it.value }
                    count += list.size
                    SemesterAverage(
                        semester = semester,
                        average = list.averageOrNull() ?: 0.0,
                        cumulative = sum.toDouble() / count,
                        marks = list.size
                    )
                }

            val currentSubjects = subjects.getOrNull(current).orEmpty()

            return AcademicProgress(
                marks = marks,
                semesters = semesters,
                semesterCount = subjects.size,
                currentSemester = current,
                session = currentSubjects.sessionProgress(current),
                totalRetakes = subjects.sumOf { list -> list.sumOf { it.examRetakes + it.creditRetakes } },
                upcomingExams = currentSubjects.count { it.hasExam && it.examMark == null },
            )
        }

        internal fun exampleMarks(needed: Double, count: Int): List<Int> {
            val points = ceil(needed - 1e-9).toInt()
                .coerceIn(PassingMark * count, MarkRange.last * count)
            val base = points / count
            val extra = points % count
            return List(count) { if (it < extra) base + 1 else base }
        }
    }
}

private fun Subject.marks(semester: Int): List<Mark> = buildList {
    if (hasExam && examMark in MarkRange)
        add(Mark(name, semester, MarkKind.Exam, examMark!!, examRetakes))
    if (hasCredit && creditMark in MarkRange)
        add(Mark(name, semester, MarkKind.Credit, creditMark!!, creditRetakes))
}

private fun List<Subject>.sessionProgress(semester: Int): SessionProgress? {
    val exams = filter { it.hasExam }
    val credits = filter { it.hasCredit }
    if (exams.isEmpty() && credits.isEmpty())
        return null

    val examPassed = { s: Subject -> s.examMark != null && s.examMark >= PassingMark }
    val creditPassed = { s: Subject ->
        s.creditPassed == true || (s.creditMark != null && s.creditMark >= PassingMark)
    }

    val pending = filter {
        (it.hasExam && !examPassed(it)) || (it.hasCredit && !creditPassed(it))
    }.map { it.name }

    return SessionProgress(
        semester = semester,
        examsTotal = exams.size,
        examsPassed = exams.count(examPassed),
        creditsTotal = credits.size,
        creditsPassed = credits.count(creditPassed),
        pending = pending
    )
}

private fun List<Mark>.averageOrNull(): Double? =
    if (isEmpty()) null else sumOf { it.value }.toDouble() / size

enum class GradeBand {
    NoMarks, Below4, From4To5, From5To6, From6To7, From7To8, From8To9, From9To95, From95To10;

    companion object {
        fun of(average: Double?): GradeBand = when {
            average == null -> NoMarks
            average < 4 -> Below4
            average < 5 -> From4To5
            average < 6 -> From5To6
            average < 7 -> From6To7
            average < 8 -> From7To8
            average < 9 -> From8To9
            average < 9.5 -> From9To95
            else -> From95To10
        }
    }
}

fun Double.formatAverage(decimals: Int = 2): String {
    var factor = 1
    repeat(decimals) { factor *= 10 }
    val scaled = (this * factor).roundToInt()
    val sign = if (scaled < 0) "-" else ""
    val abs = kotlin.math.abs(scaled)
    val int = abs / factor
    if (decimals == 0) return "$sign$int"
    val frac = (abs % factor).toString().padStart(decimals, '0')
    return "$sign$int.$frac"
}
