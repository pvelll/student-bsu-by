package github.alexzhirkevich.studentbsuby.api

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document

/**
 * Hidden state of an ASP.NET WebForms page (`__VIEWSTATE`, `__EVENTVALIDATION`, ...).
 *
 * student.bsu.by validates every postback against the state rendered on the page that is
 * being posted back, so a postback must always be built from a freshly loaded page instead
 * of hardcoded values.
 */
class AspNetForm private constructor(
    private val hiddenFields: Map<String, String>
) {

    val viewState: String get() = hiddenFields[VIEW_STATE].orEmpty()

    val isValid: Boolean get() = viewState.isNotEmpty()

    /**
     * Form body of a regular (full page) postback caused by [eventTarget]
     * (a `__doPostBack('target', 'argument')` call on the page).
     */
    fun postback(
        eventTarget: String,
        eventArgument: String = "",
        extra: Map<String, String> = emptyMap()
    ): FormUrlEncodedBody = buildMap {
        putAll(hiddenFields)
        put(EVENT_TARGET, eventTarget)
        put(EVENT_ARGUMENT, eventArgument)
        putAll(extra)
    }

    /**
     * Form body of a partial rendering (UpdatePanel / ScriptManager) postback.
     */
    fun asyncPostback(
        scriptManager: String,
        updatePanel: String,
        eventTarget: String,
        eventArgument: String = "",
        extra: Map<String, String> = emptyMap()
    ): FormUrlEncodedBody = buildMap {
        put(scriptManager, "$updatePanel|$eventTarget")
        putAll(postback(eventTarget, eventArgument, extra))
        put(ASYNC_POST, "true")
    }

    companion object {
        const val VIEW_STATE = "__VIEWSTATE"
        const val VIEW_STATE_GENERATOR = "__VIEWSTATEGENERATOR"
        const val EVENT_VALIDATION = "__EVENTVALIDATION"
        const val EVENT_TARGET = "__EVENTTARGET"
        const val EVENT_ARGUMENT = "__EVENTARGUMENT"
        const val ASYNC_POST = "__ASYNCPOST"

        private val stateFields = listOf(
            VIEW_STATE, VIEW_STATE_GENERATOR, EVENT_VALIDATION, "__LASTFOCUS", "__VIEWSTATEENCRYPTED"
        )

        fun parse(html: String): AspNetForm = parse(Ksoup.parse(html))

        fun parse(document: Document): AspNetForm {
            val fields = mutableMapOf<String, String>()
            stateFields.forEach { name ->
                val value = (document.getElementById(name)
                    ?: document.selectFirst("input[name=$name]"))
                    ?.attr("value")
                if (value != null) {
                    fields[name] = value
                }
            }
            return AspNetForm(fields)
        }
    }
}

/**
 * Extracts the html fragment from an ASP.NET partial rendering response
 * (`1|#||4|1234|updatePanel|<id>|<html>|...`). Regular html is returned untouched.
 */
fun String.asyncPostbackHtml(): String {
    if (!contains("|updatePanel|"))
        return this
    val start = indexOf('<')
    val end = lastIndexOf('>')
    if (start == -1 || end == -1 || end <= start)
        return this
    return substring(start, end + 1)
}
