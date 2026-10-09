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

val networkModule = module {

    single(named("BaseUrl")) { "https://student.bsu.by" }

    single { PersistentCookiesStorage(get(named("CookiesPrefs"))) }

    single<LoginCookieManager> { get<PersistentCookiesStorage>() }

    single { createHttpClient(get<PersistentCookiesStorage>(), get()) }

    single<LoginApi> { LoginApiWrapper(LoginApiImpl(get())) }

    single<ProfileApi> { ProfileApiImpl(get()) }

    single<TimetableApi> { TimetableApiWrapper(TimetableApiImpl(get())) }

    single<PaidServicesApi> { PaidServicesApiImpl(get()) }
}
