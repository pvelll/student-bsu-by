@file:OptIn(ExperimentalNativeApi::class)

package github.sushkpavel.studentbsuby.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.engine.darwin.DarwinClientEngineConfig
import platform.Foundation.NSProcessInfo
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

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

private fun debugProxy(): Pair<String, Int>? {
    if (!Platform.isDebugBinary)
        return null
    val value = NSProcessInfo.processInfo.environment["STUDENTBSUBY_HTTP_PROXY"] as? String
        ?: return null
    val port = value.substringAfter(':', "").toIntOrNull() ?: return null
    return value.substringBefore(':') to port
}
