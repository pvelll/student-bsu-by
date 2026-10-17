package github.sushkpavel.studentbsuby.demo

import github.sushkpavel.studentbsuby.api.ProfileApi
import github.sushkpavel.studentbsuby.demo.DemoData.Credit
import github.sushkpavel.studentbsuby.demo.DemoData.Exam

/**
 * Pages of the demo student.bsu.by. They copy the markup the app parses (element ids,
 * css classes, table layouts and ASP.NET hidden fields) of the real pages.
 */
internal object DemoPages {

    private const val LOGIN_NAME = "ctl00\$ContentPlaceHolder0\$"
    private const val LOGIN_ID = "ctl00_ContentPlaceHolder0_"
    private const val CABINET_ID = "ctl00_ctl00_ContentPlaceHolder0_"
    private const val CONTENT_ID = "${CABINET_ID}ContentPlaceHolder1_"
    private const val PROGRESS_ID = "${CONTENT_ID}ctlStudProgress1_"
    private const val PROGRESS_NAME = "ctl00\$ctl00\$ContentPlaceHolder0\$ContentPlaceHolder1\$ctlStudProgress1\$"
    private const val SCHEDULE_ID = "${CONTENT_ID}ctlSchedule1_"
    private const val SCHEDULE_NAME = "ctl00\$ctl00\$ContentPlaceHolder0\$ContentPlaceHolder1\$ctlSchedule1\$"
    private const val PAY_ID = "ctl00_ctl00_ctl00_ContentPlaceHolder0_ContentPlaceHolder1_ContentPlaceHolder1_"

    const val VIEW_STATE = "/wEPDwUKMTY0NjEzMjQ1Mg9kFgJmD2QWAgIDD2QWAgIBD2QWAmYPZBYCAgEPDxYCHgRUZXh0BQfQlNC10LzQvmRkZA=="
    private const val VIEW_STATE_GENERATOR = "C2EE9ABB"
    private const val EVENT_VALIDATION = "/wEdAAU0kGd2mIqyJmBGm0AqSzBrK7Y8z2rT5m8DhQ0W1Hh7Kg=="

    const val SCHEDULE_UPDATE_PANEL = "${SCHEDULE_ID}UpdatePanel1"

    // ---------------------------------------------------------------- login

    fun login(error: String = ""): String = page(
        title = "Вход в систему",
        content = """
<table class="LineMain" width="100%"><tr><td align="center">
<div><font size="3" color="#00285F">Вход в личный кабинет студента</font></div>
<table>
<tr><td>Логин</td><td><input name="${LOGIN_NAME}txtUserLogin" type="text" id="${LOGIN_ID}txtUserLogin" /></td></tr>
<tr><td>Пароль</td><td><input name="${LOGIN_NAME}txtUserPassword" type="password" id="${LOGIN_ID}txtUserPassword" /></td></tr>
<tr><td colspan="2"><img id="${LOGIN_ID}imgCapture" src="Captcha/CaptchaImage.aspx" alt="" /></td></tr>
<tr><td>Символы с картинки</td><td><input name="${LOGIN_NAME}txtCapture" type="text" id="${LOGIN_ID}txtCapture" /></td></tr>
<tr><td colspan="2"><input type="submit" name="${LOGIN_NAME}btnLogon" value="Войти" id="${LOGIN_ID}btnLogon" /></td></tr>
</table>
<span id="${LOGIN_ID}lbLoginResult" class="style1">$error</span>
</td></tr></table>
"""
    )

    /** login.aspx of a signed in student. */
    fun loggedIn(): String = page(
        title = "Вход в систему",
        content = """
<table class="LineMain" width="100%"><tr><td align="center">
<span id="${LOGIN_ID}lbLoginResult" class="style1">Вы вошли в систему как ${DemoData.NAME}</span>
<br />
<a id="${LOGIN_ID}LoginStatus1" href="javascript:__doPostBack(&#39;${LOGIN_NAME}LoginStatus1${'$'}ctl00&#39;,&#39;&#39;)">Выход</a>
</td></tr></table>
"""
    )

    // ---------------------------------------------------------------- marks

    /** @param selected zero based index of the shown session, null for "Все сессии". */
    fun studProgress(selected: Int?): String {
        val sessions = DemoData.sessions
        val shown = if (selected == null) sessions.indices.toList() else listOf(selected)

        val selector = sessions.withIndex().groupBy { it.value.course }.entries.joinToString("\n") { (course, list) ->
            "<tr><th class=\"styleSessionSel\">$course курс</th></tr>\n" + list.joinToString("\n") { (index, session) ->
                "<tr><td>${sessionLink("selSemester${index + 1}", session.title, bold = index == selected)}</td></tr>"
            }
        }

        val progress = shown.joinToString("\n") { index ->
            val session = sessions[index]
            """
<tr><td colspan="10" align="center"><b>${session.caption}</b></td></tr>
<tr>
<td align="center" class="styleNumberBody" title="" rowspan="2"><b>№ п/п</b></td>
<td align="left" class="styleLessonBody" title="" rowspan="2"><b>Предмет</b></td>
<td align="center" class="styleHoursBody" title="" colspan="6"><b>Часы</b></td>
<td align="center" class="styleZachBody" title="" rowspan="2"><b>Зачет</b></td>
<td align="center" class="styleExamBody" title="" rowspan="2"><b>Экзамен</b></td>
</tr>
<tr>
<td align="center" class="styleHoursSmallBody" title="лекции">лекц</td>
<td align="center" class="styleHoursSmallBody" title="практические занятий">практ</td>
<td align="center" class="styleHoursSmallBody" title="лабораторные">лаб</td>
<td align="center" class="styleHoursSmallBody" title="семинары">сем</td>
<td align="center" class="styleHoursSmallBody" title="факультативы">факул</td>
<td align="center" class="styleHoursSmallBody" title="контролируемая самостоятельная работа">КСР</td>
</tr>
""" + session.subjects.withIndex().joinToString("\n") { (number, subject) ->
                val hours = subject.hours.joinToString("") {
                    "<td align=\"center\" class=\"styleHoursSmallBody\" title=\"\">${if (it > 0) it else "&nbsp;"}</td>"
                }
                "<tr>" +
                        "<td align=\"center\" class=\"styleNumberBody\" title=\"\">${number + 1}</td>" +
                        "<td align=\"left\" class=\"styleLessonBody\" title=\"\">${subject.name}</td>" +
                        hours +
                        "<td align=\"center\" class=\"styleZachBody\" title=\"\">${subject.credit.html()}</td>" +
                        "<td align=\"center\" class=\"styleExamBody\" title=\"\">${subject.exam.html()}</td>" +
                        "</tr>"
            }
        }

        return cabinet(
            title = "Успеваемость",
            content = """
<div id="${PROGRESS_ID}UpdatePanel1">
<table border="0" width="100%">
<tr><td width="100%" valign="top">
<span id="${PROGRESS_ID}lbStudFacultet" style="display:inline-block;width:100%;">${DemoData.FACULTY}</span>
<span id="${PROGRESS_ID}lbStudKurs" style="display:inline-block;width:100%;">${DemoData.INFO}</span>
<span id="${PROGRESS_ID}lbStudBall" style="display:inline-block;width:100%;">${DemoData.averageText}</span>
</td></tr>
<tr><td width="100%" valign="top">
<br />
${sessionLink("selSemester", "Все сессии", bold = selected == null)}
<table id="${PROGRESS_ID}tblSemester" border="0" cellpadding="2" cellspacing="7">
$selector
</table>
</td></tr>
<tr><td width="100%" valign="top">
<table id="${PROGRESS_ID}tblProgress" cols="10" border="1" cellspacing="0" style="border-width:1px; border-color:Black; ">
$progress
</table>
</td></tr>
</table>
</div>
"""
        )
    }

    private fun sessionLink(id: String, text: String, bold: Boolean): String =
        "<a id=\"$PROGRESS_ID$id\" href=\"javascript:__doPostBack(&#39;$PROGRESS_NAME$id&#39;,&#39;&#39;)\" " +
                "style=\"font-size:Larger;font-weight:${if (bold) "bold" else "normal"};\">$text</a>"

    private fun Credit.html(): String = when (this) {
        Credit.None -> "&nbsp;"
        Credit.Pending -> "<i>зачет</i>"
        Credit.Passed -> "зачтено"
        is Credit.Mark -> value.toString()
    }

    private fun Exam.html(): String = when (this) {
        Exam.None -> "&nbsp;"
        Exam.Pending -> "<i>экзамен</i>"
        is Exam.Mark -> value.toString() + "'".repeat(retakes)
    }

    // ---------------------------------------------------------------- timetable

    fun schedule(): String = cabinet(
        title = "Расписание",
        content = """
<table border="0" width="100%"><tr>
${DemoData.weekdays.withIndex().joinToString("\n") { (index, day) ->
    "<td><a id=\"${SCHEDULE_ID}cmdDay${index + 1}\" href=\"javascript:__doPostBack(&#39;${SCHEDULE_NAME}cmdDay${index + 1}&#39;,&#39;&#39;)\">$day</a></td>"
}}
</tr></table>
<div id="$SCHEDULE_UPDATE_PANEL">
${scheduleDay(0)}
</div>
"""
    )

    /** Partial rendering response of a weekday button. */
    fun scheduleDelta(day: Int): String {
        val html = scheduleDay(day)
        return "1|#||4|${html.length}|updatePanel|$SCHEDULE_UPDATE_PANEL|$html|"
    }

    /** What the site answers to a postback it does not accept. */
    const val ERROR_REDIRECT_DELTA = "1|#||4|0|pageRedirect||%2fError.aspx|"

    private fun scheduleDay(day: Int): String =
        """<table class="styleTable" border="1" cellspacing="0" width="100%">
<tr><td colspan="7" align="center"><b>${DemoData.weekdays[day]}</b></td></tr>
<tr><th>№</th><th>Начало</th><th>Окончание</th><th>Дисциплина</th><th>Вид занятия</th><th>Аудитория</th><th>Преподаватель</th></tr>
""" + DemoData.timetable[day].joinToString("\n") {
            "<tr>" +
                    "<td class=\"styleNumber\" rowspan=\"1\">${it.number}</td>" +
                    "<td class=\"styleTimeIn\">${it.starts}</td>" +
                    "<td class=\"styleTimeOut\">${it.ends}</td>" +
                    "<td class=\"styleLesson\" rowspan=\"1\">${it.name}</td>" +
                    "<td class=\"styleKindLesson\" rowspan=\"1\">${it.type}</td>" +
                    "<td class=\"styleHall\" rowspan=\"1\">${it.place}</td>" +
                    "<td class=\"styleTeacher\" rowspan=\"1\">${it.teacher}</td>" +
                    "</tr>"
        } + "\n</table>"

    // ---------------------------------------------------------------- news

    /**
     * Every news item is a `h2` with the link to the item followed by its preview,
     * the parser skips the first element and the last one of the content cell.
     */
    fun news(): String = cabinet(
        title = "Новости",
        content = DemoData.news.joinToString("\n") {
            """
<h2><a href="${ProfileApi.URL_NEWS.substringAfterLast('/')}?id=${it.id}">${it.title}</a></h2>
<p>${it.preview}</p>
<p><a href="${ProfileApi.URL_NEWS.substringAfterLast('/')}?id=${it.id}">Подробнее...</a></p>
<br />"""
        } + "\n<div class=\"Footer\">&nbsp;</div>"
    )

    fun newsItem(id: Int): String? {
        val item = DemoData.news.firstOrNull { it.id == id } ?: return null
        return cabinet(
            title = "Новости",
            content = """
<div id="${CONTENT_ID}frmNewsItem">
<h2>${item.title}</h2>
<p><i>${item.date}</i></p>
${item.content}
</div>
"""
        )
    }

    // ---------------------------------------------------------------- hostel

    fun hostel(): String = cabinet(
        title = "Общежитие",
        content = if (DemoData.HOSTEL_PROVIDED) """
<p>Вам предоставлено место в общежитии:</p>
<span id="${CONTENT_ID}lbHouse2">${DemoData.HOSTEL_ADDRESS}</span>
""" else """
<p>Место в общежитии не предоставлено. Объявления о сдаче жилья:</p>
<table cellspacing="0" rules="all" border="1" id="${CONTENT_ID}GridView1" style="border-collapse:collapse;">
<tr>
<th align="left" scope="col">№</th><th align="left" scope="col">Телефон</th><th align="left" scope="col">Кто сдает</th>
<th align="left" scope="col">Адрес</th><th align="left" scope="col">Условия</th><th align="left" scope="col">Цена</th>
<th align="left" scope="col">Валюта</th><th align="left" scope="col">Дата</th><th align="left" scope="col">Примечание</th>
</tr>
${DemoData.hostelAdverts.withIndex().joinToString("\n") { (index, advert) ->
    "<tr><td align=\"center\" nowrap=\"nowrap\">${index + 1}</td>" +
            advert.joinToString("") { "<td align=\"left\">${it.ifEmpty { "&nbsp;" }}</td>" } +
            "</tr>"
}}
</table>
"""
    )

    fun studentCard(): String = cabinet(
        title = "Студенческий билет",
        content = "<p>${DemoData.NAME}</p><p>${DemoData.FACULTY}</p><p>${DemoData.INFO}</p>"
    )

    // ---------------------------------------------------------------- paid services

    fun payInfo(): String {
        val rows = DemoData.bills + listOf(
            // summary rows of the grid, the parser drops them
            listOf("Итого за семестр", DemoData.bills.last()[1], DemoData.bills.last()[2]),
            listOf("Итого за учебный год", DemoData.bills.last()[1], DemoData.PRICE),
            listOf("Всего к оплате", "&nbsp;", DemoData.bills.last()[2]),
        )
        return cabinet(
            title = "Платные услуги",
            content = """
<span id="${PAY_ID}lbDogNum">${DemoData.CONTRACT}</span><br />
Стоимость обучения: <span id="${PAY_ID}lPrice">${DemoData.PRICE}</span><br />
Задолженность: <span id="${PAY_ID}lDolg">${DemoData.DEBT}</span><br />
Пеня: <span id="${PAY_ID}lPeny">${DemoData.FINE}</span><br />
<table cellspacing="0" rules="all" border="1" id="${PAY_ID}gv" style="border-collapse:collapse;">
<tr><th valign="middle" scope="col">Вид оплаты</th><th valign="middle" scope="col">Срок оплаты</th><th valign="middle" scope="col">Сумма</th></tr>
${rows.joinToString("\n") { row -> "<tr>" + row.joinToString("") { "<td valign=\"middle\">$it</td>" } + "</tr>" }}
</table>
"""
        )
    }

    fun tuitionFee(): String = payReport(
        title = "Оплата за обучение",
        header = """
<tr><th rowspan="2">Учебный год</th><th rowspan="2">Срок оплаты</th><th rowspan="2">Сумма по договору</th><th colspan="3">Оплата</th><th colspan="2">Пеня</th><th rowspan="2">Задолженность</th></tr>
<tr><th>Дата</th><th>Сумма</th><th>Остаток</th><th>Дней</th><th>Сумма</th></tr>""",
        rows = DemoData.tuitionFee,
        columns = 9
    )

    fun academicDebt(): String = payReport(
        title = "Академическая задолженность",
        header = RECEIPTS_HEADER,
        rows = DemoData.academicDebt,
        columns = 5
    )

    fun hostelBills(): String = payReport(
        title = "Оплата за общежитие",
        header = "<tr><th>Срок оплаты</th><th>Сумма</th></tr>",
        rows = DemoData.hostelBills,
        columns = 2
    )

    fun commonReceipts(): String = payReport(
        title = "Прочие услуги",
        header = RECEIPTS_HEADER,
        rows = DemoData.commonReceipts,
        columns = 5
    )

    private const val RECEIPTS_HEADER =
        "<tr><th>Срок оплаты</th><th>Сумма</th><th>Остаток</th><th>Дата оплаты</th><th>Назначение</th></tr>"

    /** Header rows, the data rows and a total row: the report table is the last one of the page. */
    private fun payReport(title: String, header: String, rows: List<List<String>>, columns: Int): String =
        cabinet(
            title = title,
            content = """
<table border="1" cellspacing="0" width="100%">
$header
${rows.joinToString("\n") { row -> "<tr>" + row.joinToString("") { "<td>$it</td>" } + "</tr>" }}
<tr><td colspan="$columns" align="right"><b>Итого</b></td></tr>
</table>
"""
        )

    // ---------------------------------------------------------------- layout

    /** Personal cabinet master page: the menu, the student name and the page content. */
    private fun cabinet(title: String, content: String): String = page(
        title = title,
        content = """
<table class="LineMain" width="100%" border="0"><tr>
<td class="LeftMenu" width="200" valign="top">
<a href="News">Новости</a><br />
<a href="StudProgress">Успеваемость</a><br />
<a href="Schedule">Расписание</a><br />
<a href="Hostel">Общежитие</a><br />
<a href="Pay/Info">Платные услуги</a>
</td>
<td width="3" bgcolor="#00285F"></td>
<td width="*" valign="top">
<div align="center"><font size="3" color="#00285F"><span id="${CABINET_ID}lbFIO1">${DemoData.NAME}</span></font></div>
<div align="center"><font size="3" color="#00285F">$title</font></div>
$content
</td>
</tr></table>
"""
    )

    private fun page(title: String, content: String): String = """<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head><meta http-equiv="Content-Type" content="text/html; charset=utf-8" /><title>$title</title></head>
<body>
<form name="aspnetForm" method="post" id="aspnetForm">
<div>
<input type="hidden" name="__EVENTTARGET" id="__EVENTTARGET" value="" />
<input type="hidden" name="__EVENTARGUMENT" id="__EVENTARGUMENT" value="" />
<input type="hidden" name="__VIEWSTATE" id="__VIEWSTATE" value="$VIEW_STATE" />
</div>
$content
<div>
<input type="hidden" name="__VIEWSTATEGENERATOR" id="__VIEWSTATEGENERATOR" value="$VIEW_STATE_GENERATOR" />
<input type="hidden" name="__EVENTVALIDATION" id="__EVENTVALIDATION" value="$EVENT_VALIDATION" />
</div>
</form>
</body>
</html>
"""
}
