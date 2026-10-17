package github.sushkpavel.studentbsuby.demo

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * The made up student served by [DemoSite]. Nothing here belongs to a real person,
 * it only has to look like the content of student.bsu.by.
 */
internal object DemoData {

    @OptIn(ExperimentalTime::class)
    private val today: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    // Dates follow the running academic year, so the demo never looks outdated: 2026 for 2026/2027.
    private val year = if (today.month >= Month.SEPTEMBER) today.year else today.year - 1

    private fun daysAgo(days: Int): String = today.minus(days, DateTimeUnit.DAY).let {
        "${it.day.toString().padStart(2, '0')}.${it.month.number.toString().padStart(2, '0')}.${it.year}"
    }

    const val NAME = "Ковалев Артем Сергеевич"
    const val FACULTY = "Факультет прикладной математики и информатики"
    const val INFO = "3 курс, группа 5, форма обучения дневная, специальность: прикладная информатика"

    /**
     * true: a place in the hostel is provided (address + photo of the hostel),
     * false: the hostel screen shows the list of rent adverts instead.
     */
    const val HOSTEL_PROVIDED = true

    const val HOSTEL_ADDRESS = "ул. Курчатова 8, Общежитие 4, комната 512"

    // ---------------------------------------------------------------- marks

    sealed interface Credit {
        data object None : Credit
        /** Not taken yet, rendered in italics. */
        data object Pending : Credit
        data object Passed : Credit
        /** Graded credit. */
        data class Mark(val value: Int) : Credit
    }

    sealed interface Exam {
        data object None : Exam
        /** Not taken yet, rendered in italics. */
        data object Pending : Exam
        /** Every retake is rendered as an apostrophe after the mark. */
        data class Mark(val value: Int, val retakes: Int = 0) : Exam
    }

    class Subject(
        val name: String,
        /** лекц, практ, лаб, сем, факул, КСР. */
        val hours: List<Int>,
        val credit: Credit,
        val exam: Exam,
    )

    class Session(val course: Int, val winter: Boolean, val subjects: List<Subject>) {
        val title get() = if (winter) "зимняя сессия" else "весенняя сессия"
        val caption get() = "$course курс, $title"
    }

    private fun subject(
        name: String,
        lectures: Int = 0,
        practice: Int = 0,
        labs: Int = 0,
        seminars: Int = 0,
        ksr: Int = 0,
        credit: Credit = Credit.None,
        exam: Exam = Exam.None,
    ) = Subject(name, listOf(lectures, practice, labs, seminars, 0, ksr), credit, exam)

    private fun exam(value: Int, retakes: Int = 0) = Exam.Mark(value, retakes)

    private val passed = Credit.Passed

    val sessions = listOf(
        Session(
            course = 1, winter = true, subjects = listOf(
                subject("Математический анализ", lectures = 54, practice = 54, ksr = 6, credit = passed, exam = exam(8)),
                subject("Алгебра и теория чисел", lectures = 36, practice = 36, ksr = 4, credit = passed, exam = exam(9)),
                subject("Аналитическая геометрия", lectures = 34, practice = 34, exam = exam(7)),
                subject("Основы и методологии программирования", lectures = 34, labs = 52, ksr = 4, credit = passed, exam = exam(10)),
                subject("Введение в специальность", lectures = 18, credit = passed),
                subject("История белорусской государственности", lectures = 36, seminars = 18, ksr = 4, exam = exam(8)),
                subject("Иностранный язык", practice = 68, credit = passed),
                subject("Физическая культура", practice = 68, credit = passed),
            )
        ),
        Session(
            course = 1, winter = false, subjects = listOf(
                subject("Математический анализ", lectures = 54, practice = 54, ksr = 6, credit = passed, exam = exam(7)),
                subject("Алгебра и теория чисел", lectures = 34, practice = 34, exam = exam(8)),
                subject("Дискретная математика и математическая логика", lectures = 36, practice = 36, ksr = 4, credit = passed, exam = exam(9)),
                subject("Методы программирования", lectures = 34, labs = 52, credit = passed, exam = exam(9)),
                subject("Философия", lectures = 36, seminars = 18, exam = exam(9)),
                subject("Иностранный язык", practice = 68, credit = passed),
                subject("Физическая культура", practice = 68, credit = passed),
                subject("Учебная практика", credit = Credit.Mark(9)),
            )
        ),
        Session(
            course = 2, winter = true, subjects = listOf(
                subject("Дифференциальные уравнения", lectures = 36, practice = 36, ksr = 4, credit = passed, exam = exam(7)),
                subject("Теория вероятностей и математическая статистика", lectures = 36, practice = 36, ksr = 4, credit = passed, exam = exam(6, retakes = 1)),
                subject("Объектно-ориентированное программирование", lectures = 34, labs = 52, credit = passed, exam = exam(10)),
                subject("Базы данных", lectures = 34, labs = 34, ksr = 4, credit = passed, exam = exam(9)),
                subject("Архитектура компьютеров", lectures = 34, labs = 34, credit = passed),
                subject("Экономическая теория", lectures = 36, seminars = 18, exam = exam(8)),
                subject("Иностранный язык", practice = 68, credit = passed),
                subject("Физическая культура", practice = 68, credit = passed),
            )
        ),
        Session(
            course = 2, winter = false, subjects = listOf(
                subject("Численные методы", lectures = 36, labs = 36, ksr = 4, credit = passed, exam = exam(8)),
                subject("Операционные системы", lectures = 34, labs = 34, credit = passed, exam = exam(9)),
                subject("Компьютерные сети", lectures = 34, labs = 34, credit = passed, exam = exam(8)),
                subject("Алгоритмы и структуры данных", lectures = 36, practice = 18, labs = 36, exam = exam(10)),
                subject("Веб-программирование", lectures = 18, labs = 52, credit = Credit.Mark(9)),
                subject("Иностранный язык", practice = 68, exam = exam(9)),
                subject("Физическая культура", practice = 68, credit = passed),
                subject("Учебная практика", credit = passed),
            )
        ),
        // The running session: nothing is taken yet.
        Session(
            course = 3, winter = true, subjects = listOf(
                subject("Методы оптимизации", lectures = 36, practice = 36, ksr = 4, credit = Credit.Pending, exam = Exam.Pending),
                subject("Теория алгоритмов", lectures = 34, practice = 34, exam = Exam.Pending),
                subject("Разработка мобильных приложений", lectures = 34, labs = 52, credit = Credit.Pending, exam = Exam.Pending),
                subject("Машинное обучение", lectures = 36, labs = 36, ksr = 4, credit = Credit.Pending, exam = Exam.Pending),
                subject("Компьютерная графика", lectures = 34, labs = 34, credit = Credit.Pending),
                subject("Проектирование программного обеспечения", lectures = 34, practice = 34, credit = Credit.Pending),
                subject("Физическая культура", practice = 68, credit = Credit.Pending),
            )
        ),
    )

    /** Zero based index of the session the marks page opens with. */
    val currentSession get() = sessions.lastIndex

    /** "Средний балл: 8.45" — every numeric mark (exams and graded credits). */
    val averageText: String
        get() {
            val marks = sessions.flatMap { it.subjects }.flatMap {
                listOfNotNull(
                    (it.exam as? Exam.Mark)?.value,
                    (it.credit as? Credit.Mark)?.value
                )
            }
            val scaled = (marks.sum() * 100.0 / marks.size).roundToInt()
            return "Средний балл: ${scaled / 100}.${(scaled % 100).toString().padStart(2, '0')}"
        }

    // ---------------------------------------------------------------- timetable

    class Lesson(
        val number: Int,
        val name: String,
        val type: String,
        val place: String,
        val teacher: String,
    ) {
        val starts get() = times[number - 1].first
        val ends get() = times[number - 1].second
    }

    private val times = listOf(
        "08:15" to "09:35",
        "09:45" to "11:05",
        "11:15" to "12:35",
        "13:00" to "14:20",
        "14:30" to "15:50",
        "16:00" to "17:20",
        "17:40" to "19:00",
    )

    val weekdays = listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота")

    private const val LECTURE = "лекция"
    private const val PRACTICE = "практическое занятие"
    private const val LAB = "лабораторное занятие"

    private const val OPTIMIZATION = "Методы оптимизации"
    private const val ALGORITHMS = "Теория алгоритмов"
    private const val MOBILE = "Разработка мобильных приложений"
    private const val ML = "Машинное обучение"
    private const val GRAPHICS = "Компьютерная графика"
    private const val DESIGN = "Проектирование программного обеспечения"
    private const val SPORT = "Физическая культура"

    /** Monday..Saturday. */
    val timetable: List<List<Lesson>> = listOf(
        listOf(
            Lesson(1, OPTIMIZATION, LECTURE, "605", "Сидоренко А.В."),
            Lesson(2, ML, LECTURE, "605", "Гусев Д.П."),
            Lesson(3, ML, LAB, "316", "Гусев Д.П."),
        ),
        listOf(
            Lesson(2, ALGORITHMS, LECTURE, "518", "Малиновская Е.Н."),
            Lesson(3, MOBILE, LECTURE, "518", "Карпович О.И."),
            Lesson(4, MOBILE, LAB, "401", "Карпович О.И."),
            Lesson(5, SPORT, PRACTICE, "спорткомплекс", "Ткаченко М.А."),
        ),
        listOf(
            Lesson(1, GRAPHICS, LECTURE, "605", "Лазаренко И.С."),
            Lesson(2, OPTIMIZATION, PRACTICE, "309", "Сидоренко А.В."),
            Lesson(3, DESIGN, LECTURE, "518", "Белова Н.Г."),
        ),
        listOf(
            Lesson(2, ALGORITHMS, PRACTICE, "312", "Малиновская Е.Н."),
            Lesson(3, GRAPHICS, LAB, "316", "Лазаренко И.С."),
            Lesson(4, DESIGN, PRACTICE, "401", "Белова Н.Г."),
        ),
        listOf(
            Lesson(1, ML, LECTURE, "605", "Гусев Д.П."),
            Lesson(2, MOBILE, LAB, "401", "Карпович О.И."),
            Lesson(3, OPTIMIZATION, PRACTICE, "309", "Сидоренко А.В."),
            Lesson(4, SPORT, PRACTICE, "спорткомплекс", "Ткаченко М.А."),
        ),
        listOf(
            Lesson(2, ALGORITHMS, PRACTICE, "312", "Малиновская Е.Н."),
            Lesson(3, GRAPHICS, PRACTICE, "316", "Лазаренко И.С."),
        ),
    )

    // ---------------------------------------------------------------- news

    class NewsItem(
        val id: Int,
        val date: String,
        val title: String,
        val preview: String,
        /** Html of the news page. */
        val content: String,
    )

    /** Newest first, as on the site. */
    val news = listOf(
        NewsItem(
            id = 1206,
            date = daysAgo(3),
            title = "Расписание зимней экзаменационной сессии",
            preview = "Опубликовано расписание зачетов и экзаменов зимней сессии $year/${year + 1} учебного года.",
            content = """
                <p>Опубликовано расписание зачетов и экзаменов зимней экзаменационной сессии $year/${year + 1} учебного года.</p>
                <p>Расписание доступно в разделе «Расписание» личного кабинета и на информационных стендах факультетов.</p>
                <p>Обращаем внимание: к экзаменам допускаются студенты, сдавшие все зачеты текущей сессии.</p>
            """.trimIndent()
        ),
        NewsItem(
            id = 1205,
            date = daysAgo(9),
            title = "Оплата обучения за второй семестр",
            preview = "Напоминаем, что оплата за второй семестр $year/${year + 1} учебного года производится до 15 февраля ${year + 1} года.",
            content = """
                <p>Напоминаем студентам, обучающимся на платной основе, что оплата за второй семестр $year/${year + 1} учебного года производится до 15 февраля ${year + 1} года.</p>
                <p>Оплатить обучение можно в любом банке или через систему «Расчет» (ЕРИП). Сведения о платежах отображаются в разделе «Платные услуги».</p>
            """.trimIndent()
        ),
        NewsItem(
            id = 1204,
            date = daysAgo(13),
            title = "Конкурс студенческих научных работ",
            preview = "Принимаются заявки на участие в конкурсе научных работ студентов. Работы принимаются в деканатах до 20 ноября.",
            content = """
                <p>Принимаются заявки на участие в конкурсе научных работ студентов.</p>
                <p>Для участия необходимо:</p>
                <ul>
                    <li>подготовить работу и аннотацию к ней;</li>
                    <li>получить отзыв научного руководителя;</li>
                    <li>сдать документы в деканат до 20 ноября.</li>
                </ul>
                <p>Лучшие работы будут рекомендованы к публикации в сборнике студенческих научных трудов.</p>
            """.trimIndent()
        ),
        NewsItem(
            id = 1203,
            date = daysAgo(20),
            title = "Обновление личного кабинета",
            preview = "В личном кабинете стали доступны сведения об успеваемости за все сессии и квитанции на оплату услуг.",
            content = """
                <p>В личном кабинете студента стали доступны сведения об успеваемости за все сессии, а также квитанции на оплату обучения, проживания в общежитии и других услуг.</p>
                <p>Предложения по работе личного кабинета можно направлять в деканат своего факультета.</p>
            """.trimIndent()
        ),
        NewsItem(
            id = 1202,
            date = daysAgo(26),
            title = "День здоровья",
            preview = "Приглашаем студентов на спортивный праздник «День здоровья». Сбор участников у спорткомплекса в 10:00.",
            content = """
                <p>Приглашаем студентов на спортивный праздник «День здоровья».</p>
                <p>В программе: легкоатлетический забег, мини-футбол, волейбол и настольный теннис. Сбор участников у спорткомплекса в 10:00.</p>
            """.trimIndent()
        ),
        NewsItem(
            id = 1201,
            date = daysAgo(40),
            title = "Заселение в общежития",
            preview = "Заселение студентов в общежития студенческого городка на $year/${year + 1} учебный год завершено.",
            content = """
                <p>Заселение студентов в общежития студенческого городка на $year/${year + 1} учебный год завершено.</p>
                <p>По вопросам переселения обращайтесь к заведующим общежитиями.</p>
            """.trimIndent()
        ),
    )

    // ---------------------------------------------------------------- hostel

    /** Phone, publisher, address, conditions, price, currency, publish date, note. */
    val hostelAdverts = listOf(
        listOf("+375 (29) 555-01-23", "Ольга", "ул. Сурганова 47, кв. 15", "комната в 2-комнатной квартире, хозяйка не проживает", "250", "BYN", daysAgo(2), "только для девушек"),
        listOf("+375 (33) 555-47-80", "Игорь", "ул. Якуба Коласа 31, кв. 92", "1-комнатная квартира, есть мебель и бытовая техника", "420", "USD", daysAgo(4), "до метро 5 минут"),
        listOf("+375 (44) 555-12-09", "Наталья", "пр. Независимости 105, кв. 7", "место в комнате на двоих", "180", "BYN", daysAgo(7), ""),
        listOf("+375 (29) 555-90-31", "Сергей", "ул. Кальварийская 21, кв. 140", "2-комнатная квартира, после ремонта", "550", "USD", daysAgo(10), "можно двум студентам"),
    )

    // ---------------------------------------------------------------- paid services

    const val CONTRACT = "Договор № 1534-П/24"
    const val DEBT = "0.00"
    const val FINE = "0.00"
    const val PRICE = "2360.00"

    /** Payment, deadline, sum. */
    val bills = listOf(
        listOf("Оплата за обучение, 2 семестр $year/${year + 1}", "15.02.${year + 1}", "1180.00"),
    )

    /** Year, deadline, sum, paid on, paid, left, fine days, fine, debt. */
    val tuitionFee = listOf(
        listOf("${year - 2}/${year - 1}", "15.09.${year - 2}", "1075.00", "09.09.${year - 2}", "1075.00", "0.00", "0", "0.00", "0.00"),
        listOf("${year - 2}/${year - 1}", "15.02.${year - 1}", "1075.00", "12.02.${year - 1}", "1075.00", "0.00", "0", "0.00", "0.00"),
        listOf("${year - 1}/$year", "15.09.${year - 1}", "1130.00", "18.09.${year - 1}", "1130.00", "0.00", "3", "3.39", "0.00"),
        listOf("${year - 1}/$year", "15.02.$year", "1130.00", "10.02.$year", "1130.00", "0.00", "0", "0.00", "0.00"),
        listOf("$year/${year + 1}", "15.09.$year", "1180.00", "14.09.$year", "1180.00", "0.00", "0", "0.00", "0.00"),
        listOf("$year/${year + 1}", "15.02.${year + 1}", "1180.00", "", "", "1180.00", "0", "0.00", "0.00"),
    )

    /** Deadline, sum. */
    val hostelBills = listOf(
        listOf("10.09.$year", "41.30"),
        listOf("10.10.$year", "41.30"),
        listOf("10.11.$year", "41.30"),
        listOf("10.12.$year", "41.30"),
    )

    /** Deadline, sum, left, paid on, purpose. */
    val academicDebt = emptyList<List<String>>()

    /** Deadline, sum, left, paid on, purpose. */
    val commonReceipts = listOf(
        listOf("01.10.${year - 1}", "5.00", "0.00", "29.09.${year - 1}", "Изготовление дубликата студенческого билета"),
        listOf("20.02.$year", "26.40", "0.00", "16.02.$year", "Повторная сдача экзамена: Теория вероятностей и математическая статистика"),
        listOf("15.09.$year", "3.50", "0.00", "11.09.$year", "Оформление читательского билета"),
    )
}
