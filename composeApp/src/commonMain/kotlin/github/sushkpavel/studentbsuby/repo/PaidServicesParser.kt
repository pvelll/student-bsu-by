package github.sushkpavel.studentbsuby.repo

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import github.sushkpavel.studentbsuby.data.models.Bill
import github.sushkpavel.studentbsuby.data.models.PaidServicesInfo
import github.sushkpavel.studentbsuby.data.models.Receipt
import github.sushkpavel.studentbsuby.data.models.TuitionFeePayment
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlin.time.ExperimentalTime

object PaidServicesParser {

    private const val INFO_ID = "ctl00_ctl00_ctl00_ContentPlaceHolder0_ContentPlaceHolder1_ContentPlaceHolder1_"

    fun parseInfo(html: String, owner: String): Pair<PaidServicesInfo, List<Bill>>? {
        val document = Ksoup.parse(html)
        fun label(id: String) = document.getElementById(INFO_ID + id)?.text()

        val contractNumber = label("lbDogNum")
            ?.split(whitespace)?.lastOrNull(String::isNotEmpty) ?: return null
        val debt = label("lDolg")?.let(::parseAmountOrZero) ?: return null
        val fine = label("lPeny")?.let(::parseAmountOrZero) ?: return null
        val price = label("lPrice")?.let(::parseAmount)

        val bills = document.getElementById(INFO_ID + "gv")
            ?.getElementsByAttributeValue("valign", "middle")
            ?.filter { !it.hasAttr("scope") }
            ?.map(Element::cellText)
            ?.chunked(3)
            .orEmpty()
            .dropLast(1)
            .filter { it.size == 3 }
            .dropLast(2)
            .mapNotNull { row ->
                Bill(
                    owner = owner,
                    paymentType = row[0],
                    deadline = parsePaidDate(row[1]) ?: return@mapNotNull null,
                    price = parseAmount(row[2]) ?: 0f,
                    billType = Bill.TYPE_PAIDINFO
                )
            }

        return PaidServicesInfo(
            owner = owner,
            contractNumber = contractNumber,
            debt = debt,
            fine = fine,
            price = price,
        ) to bills
    }

    fun parseTuitionFees(html: String, owner: String): List<TuitionFeePayment>? =
        tableRows(html, header = 2)?.mapNotNull { cells ->
            if (cells.size != 9)
                return@mapNotNull null
            TuitionFeePayment(
                owner = owner,
                year = cells[0],
                deadline = parsePaidDate(cells[1]) ?: return@mapNotNull null,
                fullPrice = parseAmount(cells[2]) ?: return@mapNotNull null,
                date = parsePaidDate(cells[3]),
                price = parseAmount(cells[4]),
                left = parseAmount(cells[5]),
                fineDays = cells[6].filter(Char::isDigit).toIntOrNull() ?: 0,
                fineSize = parseAmount(cells[7]) ?: 0f,
                debt = parseAmount(cells[8])
            )
        }

    fun parseReceipts(html: String, owner: String, type: Int): List<Receipt>? =
        tableRows(html, header = 1)?.mapNotNull { cells ->
            if (cells.size != 5)
                return@mapNotNull null
            Receipt(
                owner = owner,
                deadline = parsePaidDate(cells[0]) ?: return@mapNotNull null,
                price = parseAmount(cells[1]),
                left = parseAmount(cells[2]),
                date = parsePaidDate(cells[3]),
                info = cells[4],
                receiptType = type,
            )
        }

    fun parseHostelBills(html: String, owner: String, paymentType: String): List<Bill>? =
        tableRows(html, header = 1)?.mapNotNull { cells ->
            if (cells.size != 2)
                return@mapNotNull null
            Bill(
                owner = owner,
                deadline = parsePaidDate(cells[0]) ?: return@mapNotNull null,
                price = parseAmount(cells[1]) ?: return@mapNotNull null,
                billType = Bill.TYPE_HOSTEL,
                paymentType = paymentType
            )
        }

    /** Cell texts of the last table on the page, without [header] rows and the totals row. */
    private fun tableRows(html: String, header: Int): List<List<String>>? =
        Ksoup.parse(html)
            .getElementsByTag("tbody")
            .lastOrNull()
            ?.getElementsByTag("tr")
            ?.drop(header)
            ?.dropLast(1)
            ?.map { row -> row.getElementsByTag("td").map(Element::cellText) }
}

private val whitespace = Regex("""\s+""")

private val paidDate = Regex("""(\d{1,2})\.(\d{1,2})\.(\d{4})""")

private fun Element.cellText(): String =
    text().replace(' ', ' ').trim()

@OptIn(ExperimentalTime::class)
internal fun parsePaidDate(text: String): Long? {
    val (day, month, year) = paidDate.find(text)?.destructured ?: return null
    val date = runCatching { LocalDate(year.toInt(), month.toInt(), day.toInt()) }.getOrNull()
        ?: return null
    return date.atTime(LocalTime(12, 0))
        .toInstant(TimeZone.currentSystemDefault())
        .toEpochMilliseconds()
}

internal fun parseAmount(text: String): Float? {
    val number = text
        .filter { it.isDigit() || it == ',' || it == '.' || it == '-' }
        .trim('.', ',')
    if (number.none(Char::isDigit))
        return null
    val separator = number.lastIndexOfAny(charArrayOf('.', ','))
    val normalized = if (separator == -1) number else
        number.substring(0, separator).filter { it.isDigit() || it == '-' } +
                '.' + number.substring(separator + 1)
    return normalized.toFloatOrNull()
}

private fun parseAmountOrZero(text: String): Float? =
    if (text.none(Char::isDigit)) 0f else parseAmount(text)
