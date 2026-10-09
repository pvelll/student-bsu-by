@file:OptIn(ExperimentalNativeApi::class)

package github.sushkpavel.studentbsuby

import androidx.compose.ui.window.ComposeUIViewController
import com.russhwolf.settings.ObservableSettings
import github.sushkpavel.studentbsuby.di.initKoin
import github.sushkpavel.studentbsuby.di.platformModule
import github.sushkpavel.studentbsuby.util.PersistentCookiesStorage
import github.sushkpavel.studentbsuby.workers.BackgroundSyncScheduler
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.http.Url
import kotlinx.coroutines.runBlocking
import org.koin.core.Koin
import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatformTools
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

fun MainViewController(): UIViewController {
    initKoinIfNeeded()
    return ComposeUIViewController {
        App()
    }
}

private fun initKoinIfNeeded() {
    if (KoinPlatformTools.defaultContext().getOrNull() != null)
        return
    val koin = initKoin(platformModules = listOf(platformModule)).koin
    clearKeychainAfterReinstall(koin)
    koin.get<BackgroundSyncScheduler>().register()
    seedDebugSession(koin)
}

private const val KEY_KEYCHAIN_OWNED = "keychain_owned_by_install"

private fun clearKeychainAfterReinstall(koin: Koin) {
    if (!UIApplication.sharedApplication.protectedDataAvailable)
        return
    val defaults = NSUserDefaults.standardUserDefaults
    if (defaults.boolForKey(KEY_KEYCHAIN_OWNED))
        return
    koin.get<ObservableSettings>(named("CredentialsPrefs")).clear()
    koin.get<ObservableSettings>(named("CookiesPrefs")).clear()
    defaults.setBool(true, forKey = KEY_KEYCHAIN_OWNED)
}

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
