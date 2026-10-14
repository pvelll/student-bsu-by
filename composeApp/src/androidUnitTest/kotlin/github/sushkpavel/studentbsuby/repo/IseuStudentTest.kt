package github.sushkpavel.studentbsuby.repo

import github.sushkpavel.studentbsuby.data.models.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IseuStudentTest {

    private val institute = "Учреждение образования «Международный государственный " +
            "экологический институт имени А.Д.Сахарова» Белорусского государственного университета"

    private fun user(faculty: String = institute, info: String) =
        User(username = "user", faculty = faculty, info = info)

    @Test
    fun `day student profile`() {
        val student = IseuStudent.from(
            user(info = "1 курс, группа А61ИТ2, форма обучения дневная, " +
                    "специальность: информационные системы и технологии ФМОС")
        )
        assertEquals(
            IseuStudent(group = "А61ИТ2", course = 1, studyForm = "дневная", abbreviations = setOf("ФМОС")),
            student
        )
    }

    @Test
    fun `extramural student profile with a colon and without the faculty abbreviation`() {
        val student = IseuStudent.from(
            user(info = "3 курс, группа З31ЭК1, форма обучения: заочная, специальность: экология")
        )
        assertEquals(
            IseuStudent(group = "З31ЭК1", course = 3, studyForm = "заочная", abbreviations = emptySet()),
            student
        )
    }

    @Test
    fun `profile without a group keeps the student recognized`() {
        val student = IseuStudent.from(user(info = "1 курс, форма обучения дневная"))

        assertEquals("", student?.group)
        assertEquals(1, student?.course)
    }

    @Test
    fun `short institute names are recognized`() {
        val info = "1 курс, группа А61ИТ2"
        assertEquals("А61ИТ2", IseuStudent.from(user("МГЭИ им. А.Д. Сахарова БГУ", info))?.group)
        assertEquals("А61ИТ2", IseuStudent.from(user("МЕЖДУНАРОДНЫЙ ГОСУДАРСТВЕННЫЙ ЭКОЛОГИЧЕСКИЙ ИНСТИТУТ", info))?.group)
    }

    @Test
    fun `students of the university faculties are not institute students`() {
        val info = "2 курс, группа 5, форма обучения дневная"
        assertNull(IseuStudent.from(user("Факультет прикладной математики и информатики", info)))
        assertNull(IseuStudent.from(user("Экономический факультет", info)))
        assertNull(IseuStudent.from(user("", info)))
    }

    @Test
    fun `group name and mixed case words are not abbreviations`() {
        val student = IseuStudent.from(
            user(info = "1 курс, группа А61ИТ2, форма обучения дневная, специальность: ИТ-технологии ФЭМ, Минск")
        )
        assertEquals(setOf("ФЭМ"), student?.abbreviations)
    }
}
