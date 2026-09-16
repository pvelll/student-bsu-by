@file:OptIn(ExperimentalNativeApi::class)

package github.alexzhirkevich.studentbsuby

import androidx.compose.ui.window.ComposeUIViewController
import com.russhwolf.settings.ObservableSettings
import github.alexzhirkevich.studentbsuby.di.initKoin
import github.alexzhirkevich.studentbsuby.di.platformModule
import github.alexzhirkevich.studentbsuby.util.PersistentCookiesStorage
import github.alexzhirkevich.studentbsuby.workers.BackgroundSyncScheduler
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.http.Url
import kotlinx.coroutines.runBlocking
import org.koin.core.Koin
import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatformTools
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIViewController
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

fun MainViewController(): UIViewController {
    initKoinIfNeeded()
    return ComposeUIViewController {
        App()
    }
}

/**
 * Starts Koin exactly once (idempotent: skipped when a Koin context is already
 * running). [BackgroundSyncScheduler.register] must run before the application
 * finishes launching — BGTaskScheduler requirement.
 */
private fun initKoinIfNeeded() {
    if (KoinPlatformTools.defaultContext().getOrNull() != null)
        return
    val koin = initKoin(platformModules = listOf(platformModule)).koin
    koin.get<BackgroundSyncScheduler>().register()
    seedDebugSession(koin)
}

/**
 * Debug binaries only (simulator testing, the site is reachable from Belarus only):
 * `STUDENTBSUBY_DEBUG_COOKIE="ASP.NET_SessionId=...; AuthCookie=..."` and
 * `STUDENTBSUBY_DEBUG_USERNAME=login` in the process environment restore an existing
 * session without going through the login form.
 */
private fun seedDebugSession(koin: Koin) {
    if (!Platform.isDebugBinary)
        return
    val environment = NSProcessInfo.processInfo.environment
    val cookies = environment["STUDENTBSUBY_DEBUG_COOKIE"] as? String ?: return
    val username = environment["STUDENTBSUBY_DEBUG_USERNAME"] as? String ?: "debug"

    val storage = koin.get<PersistentCookiesStorage>()
    val url = Url("https://student.bsu.by/")
    runBlocking {
        cookies.split(';')
            .map(String::trim)
            .filter { '=' in it }
            .forEach { pair ->
                storage.addCookie(
                    url,
                    Cookie(
                        name = pair.substringBefore('='),
                        value = pair.substringAfter('='),
                        encoding = CookieEncoding.RAW,
                        path = "/"
                    )
                )
            }
    }
    koin.get<ObservableSettings>(named("CredentialsPrefs")).apply {
        putString("username", username)
        putBoolean("autoLogin", true)
        println("D/DebugSession: seeded username='${getStringOrNull("username")}' autoLogin=${getBooleanOrNull("autoLogin")}")
    }
}
