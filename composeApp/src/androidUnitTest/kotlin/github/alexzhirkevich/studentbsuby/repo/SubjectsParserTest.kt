package github.alexzhirkevich.studentbsuby.repo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SubjectsParserTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(name)) { "missing fixture $name" }
            .bufferedReader().use { it.readText() }

    @Test
    fun `all sessions page is split into semesters by caption rows`() {
        val semesters = SubjectsParser.parse(fixture("stud_progress_all_sessions.html"), "user")

        assertEquals(2, semesters.size)
        assertEquals(13, semesters[0].size)
        assertEquals(13, semesters[1].size)
        assertTrue(semesters[0].all { it.semester == 1 && it.owner == "user" })
        assertTrue(semesters[1].all { it.semester == 2 })

        val first = semesters[0][0]
        assertEquals("Основы алгоритмизации и программирования", first.name)
        assertTrue(first.hasCredit)
        assertTrue(first.hasExam)
        // not taken yet: rendered in italics, no mark
        assertNull(first.creditPassed)
        assertNull(first.creditMark)
        assertNull(first.examMark)
        assertEquals(0, first.creditRetakes)

        val biology = semesters[0][2]
        assertEquals("Биология", biology.name)
        assertTrue(biology.hasCredit)
        assertFalse(biology.hasExam)

        val history = semesters[0][9]
        assertEquals("История белорусской государственности", history.name)
        assertFalse(history.hasCredit)
        assertTrue(history.hasExam)
    }

    @Test
    fun `partial rendering response is parsed too`() {
        val semesters = SubjectsParser.parse(fixture("stud_progress_all_sessions_async.txt"), "user")

        assertEquals(2, semesters.size)
        assertEquals(13, semesters[0].size)
        assertEquals(13, semesters[1].size)
    }

    @Test
    fun `current session page yields a single semester`() {
        val semesters = SubjectsParser.parse(fixture("stud_progress_current.html"), "user")

        assertEquals(1, semesters.size)
        assertEquals(13, semesters[0].size)
    }

    @Test
    fun `marks and retakes are read from the cells`() {
        val html = """
            <table id="ctl00_tblProgress">
              <tr><td colspan="10"><b>2 курс, зимняя сессия</b></td></tr>
              <tr>
                <td class="styleNumberBody">1</td><td class="styleLessonBody">Алгебра</td>
                <td>34</td><td>&nbsp;</td><td>17</td><td>&nbsp;</td><td>&nbsp;</td><td>4</td>
                <td class="styleZachBody">зачтено</td><td class="styleExamBody">8'</td>
              </tr>
              <tr>
                <td class="styleNumberBody">2</td><td class="styleLessonBody">Физика</td>
                <td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td>
                <td class="styleZachBody"><font color="red">не зачтено</font></td><td class="styleExamBody">&nbsp;</td>
              </tr>
              <tr>
                <td class="styleNumberBody">3</td><td class="styleLessonBody">Химия</td>
                <td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td>
                <td class="styleZachBody">3</td><td class="styleExamBody"><i>экзамен</i></td>
              </tr>
            </table>
        """.trimIndent()

        val semesters = SubjectsParser.parse(html, "user")
        assertEquals(1, semesters.size)
        val (algebra, physics, chemistry) = semesters[0]

        assertEquals(34, algebra.lectures)
        assertEquals(17, algebra.labs)
        assertEquals(4, algebra.ksr)
        assertEquals(true, algebra.creditPassed)
        assertEquals(8, algebra.examMark)
        assertEquals(1, algebra.examRetakes)

        assertEquals(false, physics.creditPassed)
        assertFalse(physics.hasExam)

        assertEquals(3, chemistry.creditMark)
        assertEquals(false, chemistry.creditPassed)
        assertTrue(chemistry.hasExam)
        assertNull(chemistry.examMark)
    }

    @Test
    fun `pages without captions split semesters when numbering restarts`() {
        fun row(n: Int, name: String) = """
            <tr><td>$n</td><td>$name</td><td></td><td></td><td></td><td></td><td></td><td></td><td>зачет</td><td></td></tr>
        """
        val html = "<table id='x_tblProgress'>" + row(1, "A") + row(2, "B") + row(1, "C") + "</table>"

        val semesters = SubjectsParser.parse(html, "user")
        assertEquals(listOf(listOf("A", "B"), listOf("C")), semesters.map { s -> s.map { it.name } })
    }

    @Test
    fun `current semester is the bold session link`() {
        assertEquals(0, SubjectsParser.currentSemesterIndex(fixture("stud_progress_current.html")))
        // "Все сессии" selected: no particular semester
        assertNull(SubjectsParser.currentSemesterIndex(fixture("stud_progress_all_sessions.html")))
    }
}
