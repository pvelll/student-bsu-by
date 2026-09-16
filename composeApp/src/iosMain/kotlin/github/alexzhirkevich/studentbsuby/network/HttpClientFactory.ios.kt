@file:OptIn(ExperimentalNativeApi::class)

package github.alexzhirkevich.studentbsuby.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.engine.darwin.DarwinClientEngineConfig
import platform.Foundation.NSProcessInfo
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

// The Darwin engine never follows redirects natively (its NSURLSession delegate
// cancels them), so redirect handling is fully controlled by followRedirects = false.
// Native cookie handling is disabled entirely: Ktor's HttpCookies plugin owns cookies.
actual fun httpClientEngine() : HttpClientEngineFactory<*> =
    object : HttpClientEngineFactory<DarwinClientEngineConfig> {
        override fun create(block: DarwinClientEngineConfig.() -> Unit): HttpClientEngine =
            Darwin.create {
                block()
                configureSession {
                    setHTTPShouldSetCookies(false)
                    setHTTPCookieStorage(null)
                    debugProxy()?.let { (host, port) ->
                        connectionProxyDictionary = mapOf<Any?, Any?>(
                            "HTTPEnable" to 1, "HTTPProxy" to host, "HTTPPort" to port,
                            "HTTPSEnable" to 1, "HTTPSProxy" to host, "HTTPSPort" to port,
                        )
                    }
                }
            }
    }

/**
 * Debug binaries only: `STUDENTBSUBY_HTTP_PROXY=host:port` in the process environment
 * routes the traffic through an http proxy. student.bsu.by is reachable from Belarus
 * only, so simulator runs are tunneled through a proxy on the development machine.
 */
private fun debugProxy(): Pair<String, Int>? {
    if (!Platform.isDebugBinary)
        return null
    val value = NSProcessInfo.processInfo.environment["STUDENTBSUBY_HTTP_PROXY"] as? String
        ?: return null
    val port = value.substringAfter(':', "").toIntOrNull() ?: return null
    return value.substringBefore(':') to port
}
