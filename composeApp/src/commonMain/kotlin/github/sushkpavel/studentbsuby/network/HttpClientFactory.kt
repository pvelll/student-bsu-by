package github.sushkpavel.studentbsuby.network

import github.sushkpavel.studentbsuby.util.PlatformInfo
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestRetryConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.encodedPath

expect fun httpClientEngine() : HttpClientEngineFactory<*>

private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"

fun createHttpClient(
    cookiesStorage: CookiesStorage,
    platformInfo: PlatformInfo
) : HttpClient = HttpClient(httpClientEngine()) {
    bsuClientConfig(cookiesStorage, logger = Logger.SIMPLE.takeIf { platformInfo.isDebug })
}

fun createIseuHttpClient(platformInfo: PlatformInfo): HttpClient =
    HttpClient(httpClientEngine()) { iseuClientConfig(logger = Logger.SIMPLE.takeIf { platformInfo.isDebug }) }

internal fun HttpRequestRetryConfig.retryTransientFailures(retried: (HttpMethod) -> Boolean = { true }) {
    retryOnException(maxRetries = 2, retryOnTimeout = true)
    val onException = checkNotNull(retryOnExceptionIf)
    retryOnExceptionIf { request, cause -> retried(request.method) && onException(request, cause) }
    retryIf { request, response -> retried(request.method) && response.status.value in 502..504 }
    exponentialDelay(baseDelayMs = 500, maxDelayMs = 2_000, randomizationMs = 500)
}

internal fun HttpClientConfig<*>.bsuClientConfig(cookiesStorage: CookiesStorage, logger: Logger?) {

    followRedirects = false

    install(HttpCookies) {
        storage = cookiesStorage
    }

    install(HttpRequestRetry) {
        retryTransientFailures { it == HttpMethod.Get }
    }

    install(HttpTimeout) {
        requestTimeoutMillis = 30_000L
        connectTimeoutMillis = 15_000L
        socketTimeoutMillis = 30_000L
    }

    defaultRequest {
        url("https://student.bsu.by/")
        header("Connection", "close")
        header("User-Agent", USER_AGENT)
    }

    if (logger != null) {
        install(Logging) {
            this.logger = logger
            level = LogLevel.BODY
            filter { !it.url.encodedPath.endsWith("login.aspx", ignoreCase = true) }
            sanitizeHeader { it == HttpHeaders.Cookie || it == HttpHeaders.SetCookie }
        }
    }
}

internal fun HttpClientConfig<*>.iseuClientConfig(logger: Logger?) {

    followRedirects = false

    install(HttpCookies)

    install(HttpRequestRetry) {
        retryTransientFailures()
    }

    install(HttpTimeout) {
        requestTimeoutMillis = 45_000L
        connectTimeoutMillis = 20_000L
        socketTimeoutMillis = 45_000L
    }

    defaultRequest {
        header("User-Agent", USER_AGENT)
    }

    if (logger != null) {
        install(Logging) {
            this.logger = logger
            level = LogLevel.HEADERS
        }
    }
}
