package github.sushkpavel.studentbsuby.repo

import github.sushkpavel.studentbsuby.data.models.Bill
import github.sushkpavel.studentbsuby.data.models.Receipt
import github.sushkpavel.studentbsuby.util.formatTimestamp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PaidServicesParserTest {

    private val id = "ctl00_ctl00_ctl00_ContentPlaceHolder0_ContentPlaceHolder1_ContentPlaceHolder1_"

    private fun table(vararg rows: List<String>) = buildString {
        append("<html><body><table><tbody><tr><td>header</td></tr>")
        rows.forEach { row -> append("<tr>").append(row.joinToString("") { "<td>$it</td>" }).append("</tr>") }
        append("<tr><td>Итого</td></tr></tbody></table></body></html>")
    }

    private fun infoPage(
        debt: String = "0.00",
        fine: String = "0.00",
        bills: List<List<String>> = emptyList(),
    ) = buildString {
        append("<html><body>")
        append("<span id='${id}lbDogNum'>Договор № 123/24</span>")
        append("<span id='${id}lDolg'>$debt</span>")
        append("<span id='${id}lPeny'>$fine</span>")
        append("<span id='${id}lPrice'>2 950,00</span>")
        append("<table id='${id}gv'><tr>")
        listOf("Вид", "Срок", "Сумма").forEach { append("<th scope='col' valign='middle'>$it</th>") }
        append("</tr>")
        bills.forEach { row ->
            append("<tr>").append(row.joinToString("") { "<td valign='middle'>$it</td>" }).append("</tr>")
        }
        append("</table></body></html>")
    }

    @Test
    fun `dates keep their month`() {
        // dd.mm.yyyy (minutes instead of months) turned every deadline into January
        val date = assertNotNull(parsePaidDate("15.09.2025"))
        assertEquals("15.09.2025", formatTimestamp(date))
        assertEquals("01.02.2026", formatTimestamp(assertNotNull(parsePaidDate(" 1.2.2026 г."))))
        assertTrue(parsePaidDate("15.09.2025")!! > parsePaidDate("20.01.2025")!!)
        assertNull(parsePaidDate(""))
        assertNull(parsePaidDate("31.02.2025"))
    }

    @Test
    fun `amounts in any local format`() {
        assertEquals(1234.56f, parseAmount("1234.56"))
        assertEquals(1234.56f, parseAmount("1 234,56"))
        assertEquals(1234.56f, parseAmount("1 234,56 руб."))
        assertEquals(1234.56f, parseAmount("1 234,56"))
        assertEquals(1234.56f, parseAmount("1.234,56"))
        assertEquals(-12.5f, parseAmount("-12,5"))
        assertEquals(300f, parseAmount("300"))
        assertNull(parseAmount(""))
        assertNull(parseAmount("руб."))
    }

    @Test
    fun `info page with bills`() {
        val (info, bills) = assertNotNull(
            PaidServicesParser.parseInfo(
                infoPage(
                    debt = "1 475,00",
                    fine = "0,00",
                    bills = listOf(
                        listOf("Обучение", "01.09.2025", "1 475,00"),
                        listOf("Обучение", "01.02.2026", "1 475,00"),
                        listOf("Пеня", "01.02.2026", "0,00"),
                        listOf("Итого", "01.02.2026", "2 950,00"),
                        listOf("", "", ""),
                    )
                ),
                owner = "user"
            )
        )
        assertEquals("123/24", info.contractNumber)
        assertEquals(1475f, info.debt)
        assertEquals(0f, info.fine)
        assertEquals(2950f, info.price)
        // The trailing cell group and the two rows before it are not bills (as in the original app)
        assertEquals(listOf("01.09.2025", "01.02.2026"), bills.map { formatTimestamp(it.deadline) })
        assertTrue(bills.all { it.billType == Bill.TYPE_PAIDINFO && it.price == 1475f })
    }

    @Test
    fun `short bill tables do not crash`() {
        // subList(0, lastIndex - 1) threw IndexOutOfBounds here and the whole tab failed
        for (count in 0..3) {
            val rows = List(count) { listOf("Обучение", "01.09.2025", "100") }
            val (_, bills) = assertNotNull(PaidServicesParser.parseInfo(infoPage(bills = rows), "user"))
            assertEquals(maxOf(0, count - 3), bills.size)
        }
    }

    @Test
    fun `info without numbers in labels`() {
        val (info, _) = assertNotNull(PaidServicesParser.parseInfo(infoPage(debt = "нет", fine = ""), "user"))
        assertEquals(0f, info.debt)
        assertEquals(0f, info.fine)
    }

    @Test
    fun `no contract means no paid info`() {
        assertNull(PaidServicesParser.parseInfo("<html><body>Нет договора</body></html>", "user"))
    }

    @Test
    fun `receipts skip broken rows only`() {
        val receipts = assertNotNull(
            PaidServicesParser.parseReceipts(
                table(
                    listOf("01.10.2025", "35,50", "0,00", "28.09.2025", "Справка"),
                    listOf("—", "35,50", "0,00", "", "Без даты"),
                    listOf("01.11.2025", "40", "40", "", "Не оплачено"),
                ),
                owner = "user",
                type = Receipt.TYPE_COMMON
            )
        )
        assertEquals(listOf("Справка", "Не оплачено"), receipts.map { it.info })
        assertEquals("28.09.2025", formatTimestamp(assertNotNull(receipts[0].date)))
        assertEquals(35.5f, receipts[0].price)
        assertNull(receipts[1].date)
        assertEquals(40f, receipts[1].left)
    }

    @Test
    fun `tuition fees`() {
        val html = buildString {
            append("<html><body><table><tbody><tr><td>header</td></tr><tr><td>header 2</td></tr><tr>")
            listOf("2025/2026", "01.09.2025", "1 475,00", "29.08.2025", "1 475,00", "0,00", "0", "0,00", "0,00")
                .forEach { append("<td>$it</td>") }
            append("</tr><tr><td>Итого</td></tr></tbody></table></body></html>")
        }
        val payment = assertNotNull(PaidServicesParser.parseTuitionFees(html, "user")).single()
        assertEquals("2025/2026", payment.year)
        assertEquals("01.09.2025", formatTimestamp(payment.deadline))
        assertEquals(1475f, payment.fullPrice)
        assertEquals("29.08.2025", formatTimestamp(assertNotNull(payment.date)))
        assertEquals(0f, payment.left)
    }

    @Test
    fun `hostel bills`() {
        val bills = assertNotNull(
            PaidServicesParser.parseHostelBills(
                table(listOf("10.10.2025", "52,30"), listOf("10.11.2025", "")),
                owner = "user",
                paymentType = "Общежитие"
            )
        )
        assertEquals(1, bills.size)
        assertEquals(52.3f, bills[0].price)
        assertEquals(Bill.TYPE_HOSTEL, bills[0].billType)
    }
}
