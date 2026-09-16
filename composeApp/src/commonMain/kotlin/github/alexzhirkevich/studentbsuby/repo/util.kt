package github.alexzhirkevich.studentbsuby.repo

import github.alexzhirkevich.studentbsuby.api.isSessionExpired
import github.alexzhirkevich.studentbsuby.util.exceptions.EmptyResponseException
import github.alexzhirkevich.studentbsuby.util.exceptions.FailResponseException
import github.alexzhirkevich.studentbsuby.util.exceptions.SessionExpiredException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException

@Throws(
    FailResponseException::class,
    SessionExpiredException::class,
    EmptyResponseException::class,
    CancellationException::class
)
suspend fun HttpResponse.html() : String {
    if (!status.isSuccess())
        throw FailResponseException(status.value)

    val text = bodyAsText()

    if (text.isSessionExpired())
        throw SessionExpiredException()

    return text
}

/**
 * Raw body of a binary response (photo, captcha). The site answers with the login page
 * instead of an image when the session is gone, so html responses are checked for the
 * login form before the bytes are handed out.
 */
@Throws(
    FailResponseException::class,
    SessionExpiredException::class,
    EmptyResponseException::class,
    CancellationException::class
)
suspend fun HttpResponse.bytes() : ByteArray {
    if (!status.isSuccess())
        throw FailResponseException(status.value)

    val bytes = readRawBytes()

    if (bytes.isEmpty())
        throw EmptyResponseException()

    val isHtml = contentType()?.match(ContentType.Text.Html) == true ||
            bytes.startsWithIgnoringWhitespace("<!DOCTYPE", "<html")

    if (isHtml && bytes.decodeToString().isSessionExpired())
        throw SessionExpiredException()

    return bytes
}

private fun ByteArray.startsWithIgnoringWhitespace(vararg prefixes: String): Boolean {
    val head = copyOfRange(0, minOf(size, 64)).decodeToString().trimStart()
    return prefixes.any { head.startsWith(it, ignoreCase = true) }
}
