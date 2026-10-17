package github.sushkpavel.studentbsuby.demo

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineCapability
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.callContext
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.content.OutgoingContent
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.toByteArray
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.coroutines.CoroutineContext

/**
 * Engine of the student.bsu.by client: requests go to [network], or to [site] while the demo
 * mode is on. Everything above the engine (cookies, retries, parsers, caches) works the same
 * way in both modes, and the demo mode never touches the network.
 */
class DemoRoutingEngine(
    private val network: HttpClientEngine,
    private val site: DemoSite,
    private val demoMode: DemoMode,
) : HttpClientEngine {

    override val dispatcher: CoroutineDispatcher get() = network.dispatcher

    override val config: HttpClientEngineConfig get() = network.config

    override val supportedCapabilities: Set<HttpClientEngineCapability<*>>
        get() = network.supportedCapabilities

    override val coroutineContext: CoroutineContext get() = network.coroutineContext

    @InternalAPI
    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        if (!demoMode.isEnabled)
            return network.execute(data)

        val response = site.respond(data.method, data.url, data.headers, data.body.bytes())
        return HttpResponseData(
            statusCode = response.status,
            requestTime = GMTDate(),
            headers = response.headers,
            version = HttpProtocolVersion.HTTP_1_1,
            body = ByteReadChannel(response.body),
            callContext = callContext()
        )
    }

    override fun close() = network.close()
}

private suspend fun OutgoingContent.bytes(): ByteArray = when (this) {
    is OutgoingContent.ByteArrayContent -> bytes()
    is OutgoingContent.ReadChannelContent -> readFrom().toByteArray()
    else -> ByteArray(0)
}
