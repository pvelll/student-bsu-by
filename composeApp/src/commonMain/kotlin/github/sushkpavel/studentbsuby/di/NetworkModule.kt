package github.sushkpavel.studentbsuby.di

import github.sushkpavel.studentbsuby.api.LoginApi
import github.sushkpavel.studentbsuby.api.LoginApiImpl
import github.sushkpavel.studentbsuby.api.LoginApiWrapper
import github.sushkpavel.studentbsuby.api.PaidServicesApi
import github.sushkpavel.studentbsuby.api.PaidServicesApiImpl
import github.sushkpavel.studentbsuby.api.ProfileApi
import github.sushkpavel.studentbsuby.api.ProfileApiImpl
import github.sushkpavel.studentbsuby.api.TimetableApi
import github.sushkpavel.studentbsuby.api.TimetableApiImpl
import github.sushkpavel.studentbsuby.api.TimetableApiWrapper
import github.sushkpavel.studentbsuby.network.createHttpClient
import github.sushkpavel.studentbsuby.util.LoginCookieManager
import github.sushkpavel.studentbsuby.util.PersistentCookiesStorage
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Mirrors the original RetrofitModule. The Retrofit/OkHttp stack is replaced by the
 * shared Ktor [io.ktor.client.HttpClient]; api wrappers keep their stateful
 * VIEWSTATE singletons exactly like the original @Singleton providers.
 */
val networkModule = module {

    // provideBaseUrl(): android.net.Uri -> String (no trailing slash).
    single(named("BaseUrl")) { "https://student.bsu.by" }

    // ONE storage instance shared between the HttpClient cookie plugin and the
    // LoginCookieManager binding (original RetrofitModule shared the
    // PreferencesCookieCache instance for provideCookieCleaner).
    single { PersistentCookiesStorage(get(named("CookiesPrefs"))) }

    single<LoginCookieManager> { get<PersistentCookiesStorage>() }

    single { createHttpClient(get<PersistentCookiesStorage>(), get()) }

    single<LoginApi> { LoginApiWrapper(LoginApiImpl(get())) }

    single<ProfileApi> { ProfileApiImpl(get()) }

    single<TimetableApi> { TimetableApiWrapper(TimetableApiImpl(get())) }

    single<PaidServicesApi> { PaidServicesApiImpl(get()) }
}
