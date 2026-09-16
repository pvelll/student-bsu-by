package github.alexzhirkevich.studentbsuby.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AspNetFormTest {

    private val page = """
        <form>
        <input type="hidden" name="__EVENTTARGET" id="__EVENTTARGET" value="" />
        <input type="hidden" name="__VIEWSTATE" id="__VIEWSTATE" value="/wEPDw+state=" />
        <input type="hidden" name="__VIEWSTATEGENERATOR" id="__VIEWSTATEGENERATOR" value="81FD4C30" />
        <input type="hidden" name="__EVENTVALIDATION" id="__EVENTVALIDATION" value="/wEdAA==" />
        </form>
    """.trimIndent()

    @Test
    fun `postback carries the page state and the event target`() {
        val form = AspNetForm.parse(page)
        assertTrue(form.isValid)

        val body = form.postback("ctl00\$selSemester")

        assertEquals("/wEPDw+state=", body["__VIEWSTATE"])
        assertEquals("81FD4C30", body["__VIEWSTATEGENERATOR"])
        assertEquals("/wEdAA==", body["__EVENTVALIDATION"])
        assertEquals("ctl00\$selSemester", body["__EVENTTARGET"])
        assertEquals("", body["__EVENTARGUMENT"])
        assertFalse(body.containsKey("__ASYNCPOST"))
    }

    @Test
    fun `async postback adds the script manager fields`() {
        val body = AspNetForm.parse(page).asyncPostback(
            scriptManager = "sm", updatePanel = "panel", eventTarget = "target"
        )

        assertEquals("panel|target", body["sm"])
        assertEquals("true", body["__ASYNCPOST"])
        assertEquals("target", body["__EVENTTARGET"])
    }

    @Test
    fun `page without state is invalid`() {
        assertFalse(AspNetForm.parse("<html></html>").isValid)
    }

    @Test
    fun `html fragment is extracted from partial rendering responses`() {
        val delta = "1|#||4|23|updatePanel|ctl00_panel|<table><tr><td>1</td></tr></table>|0|hiddenField|__VIEWSTATE|abc|"
        assertEquals("<table><tr><td>1</td></tr></table>", delta.asyncPostbackHtml())
        assertEquals("<html/>", "<html/>".asyncPostbackHtml())
    }
}
