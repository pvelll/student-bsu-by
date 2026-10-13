package github.sushkpavel.studentbsuby.ui.screens.drawer.progress

import github.sushkpavel.studentbsuby.data.models.Subject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AcademicProgressTest {

    private fun subject(
        name: String,
        semester: Int,
        exam: Int? = null,
        hasExam: Boolean = exam != null,
        credit: Boolean? = null,
        creditMark: Int? = null,
        hasCredit: Boolean = credit != null || creditMark != null,
        examRetakes: Int = 0,
    ) = Subject(
        owner = "user",
        name = name,
        lectures = 0, practice = 0, labs = 0, seminars = 0, facults = 0, ksr = 0,
        hasCredit = hasCredit,
        creditPassed = credit,
        creditMark = creditMark,
        creditRetakes = 0,
        hasExam = hasExam,
        examMark = exam,
        examRetakes = examRetakes,
        semester = semester + 1,
    )

    private val subjects = listOf(
        listOf(
            subject("Математика", 0, exam = 8),
            subject("Физика", 0, exam = 6),
            subject("Физкультура", 0, credit = true),
            subject("Программирование", 0, creditMark = 10),
        ),
        listOf(
            subject("Математика", 1, exam = 9, examRetakes = 1),
            subject("Философия", 1, exam = 7),
        ),
        listOf(
            subject("Базы данных", 2, hasExam = true),
            subject("Английский", 2, credit = null, hasCredit = true),
        ),
    )

    @Test
    fun `averages per semester and cumulative`() {
        val progress = AcademicProgress.from(subjects, currentSemester = 2)

        assertEquals(5, progress.marks.size)
        assertEquals(8.0, progress.average!!, 1e-9)

        assertEquals(listOf(0, 1), progress.semesters.map { it.semester })
        assertEquals(8.0, progress.semesters[0].average, 1e-9)
        assertEquals(8.0, progress.semesters[1].average, 1e-9)
        assertEquals(8.0, progress.semesters[1].cumulative, 1e-9)
        assertEquals(0.0, progress.lastDelta!!, 1e-9)
        assertEquals(1, progress.excellentMarks)
    }

    @Test
    fun `marks outside the 10-point scale are ignored`() {
        val progress = AcademicProgress.from(
            listOf(listOf(subject("Сломанная ячейка", 0, exam = 27), subject("Химия", 0, exam = 5))),
            currentSemester = 0
        )
        assertEquals(listOf(5), progress.marks.map { it.value })
    }

    @Test
    fun `rating prefers exam marks and orders from the best`() {
        val rating = AcademicProgress.from(subjects, 2).rating()

        assertEquals(
            listOf("Программирование", "Математика", "Математика", "Философия", "Физика"),
            rating.map { it.name }
        )
        assertEquals(listOf(10, 9, 8, 7, 6), rating.map { it.mark })
        assertEquals(1, rating[1].semester)
    }

    @Test
    fun `distribution counts every mark`() {
        val progress = AcademicProgress.from(subjects, 2)

        assertEquals(listOf(0, 0, 0, 0, 0, 1, 1, 1, 1, 1), progress.distribution())
        assertEquals(listOf(0, 0, 0, 0, 0, 0, 1, 0, 1, 0), progress.distribution(semester = 1))
    }

    @Test
    fun `current session progress`() {
        val session = AcademicProgress.from(subjects, 2).session!!

        assertEquals(2, session.semester)
        assertEquals(1, session.examsTotal)
        assertEquals(0, session.examsPassed)
        assertEquals(1, session.creditsTotal)
        assertEquals(listOf("Базы данных", "Английский"), session.pending)
        assertEquals(1, AcademicProgress.from(subjects, 2).upcomingExams)
    }

    @Test
    fun `retakes are counted`() {
        assertEquals(1, AcademicProgress.from(subjects, 2).totalRetakes)
    }

    @Test
    fun `goal needs the missing points spread over the remaining marks`() {
        // 5 marks, sum 40; 3 more marks to reach 8.5: 8.5 * 8 - 40 = 28 points.
        val goal = AcademicProgress.from(subjects, 2).goal(target = 8.5, count = 3)

        assertIs<AcademicProgress.Goal.Reachable>(goal)
        assertEquals(28.0 / 3, goal.required, 1e-9)
        assertEquals(listOf(10, 9, 9), goal.example)
    }

    @Test
    fun `goal limits`() {
        val progress = AcademicProgress.from(subjects, 2)

        val unreachable = progress.goal(target = 9.5, count = 1)
        assertIs<AcademicProgress.Goal.Unreachable>(unreachable)
        assertEquals(50 / 6.0, unreachable.best, 1e-9)
        assertIs<AcademicProgress.Goal.AlreadyReached>(progress.goal(target = 6.0, count = 1))

        // Average 3: three more marks of 4 already lift it to 3.6.
        val low = AcademicProgress.from(
            listOf(listOf(subject("А", 0, exam = 3), subject("Б", 0, exam = 3))), 0
        )
        assertIs<AcademicProgress.Goal.AnyPassingMarks>(low.goal(target = 3.4, count = 3))
    }

    @Test
    fun `no marks yet`() {
        val progress = AcademicProgress.from(listOf(listOf(subject("Алгебра", 0, hasExam = true))), 0)

        assertNull(progress.average)
        assertTrue(progress.semesters.isEmpty())
        assertEquals(GradeBand.NoMarks, GradeBand.of(progress.average))
        assertEquals(listOf(8, 8), (progress.goal(8.0, 2) as AcademicProgress.Goal.Reachable).example)
    }

    @Test
    fun `grade bands`() {
        assertEquals(GradeBand.Below4, GradeBand.of(3.99))
        assertEquals(GradeBand.From4To5, GradeBand.of(4.0))
        assertEquals(GradeBand.From8To9, GradeBand.of(8.99))
        assertEquals(GradeBand.From9To95, GradeBand.of(9.0))
        assertEquals(GradeBand.From9To95, GradeBand.of(9.49))
        assertEquals(GradeBand.From95To10, GradeBand.of(9.5))
        assertEquals(GradeBand.From95To10, GradeBand.of(10.0))
    }

    @Test
    fun `average formatting`() {
        assertEquals("8.47", 8.4666.formatAverage())
        assertEquals("10.00", 10.0.formatAverage())
        assertEquals("-0.25", (-0.25).formatAverage())
        assertEquals("7.5", 7.5.formatAverage(decimals = 1))
    }
}
