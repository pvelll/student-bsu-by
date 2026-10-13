package github.sushkpavel.studentbsuby.di

import com.russhwolf.settings.ObservableSettings
import github.sushkpavel.studentbsuby.repo.UsernameProvider
import github.sushkpavel.studentbsuby.repo.UsernameProviderImpl
import github.sushkpavel.studentbsuby.util.Calendar
import github.sushkpavel.studentbsuby.util.CaptchaRecognizer
import github.sushkpavel.studentbsuby.util.ErrorHandler
import github.sushkpavel.studentbsuby.util.PlatformInfo
import github.sushkpavel.studentbsuby.util.ResourceManager
import github.sushkpavel.studentbsuby.util.createCaptchaRecognizer
import github.sushkpavel.studentbsuby.util.dispatchers.CoroutineJobManagerImpl
import github.sushkpavel.studentbsuby.util.dispatchers.Dispatchers
import github.sushkpavel.studentbsuby.util.dispatchers.DispatchersImpl
import github.sushkpavel.studentbsuby.util.logger.DefaultLogger
import github.sushkpavel.studentbsuby.util.logger.Logger
import github.sushkpavel.studentbsuby.util.platformInfo
import github.sushkpavel.studentbsuby.util.provideDefaultSettings
import github.sushkpavel.studentbsuby.util.provideSecureSettings
import github.sushkpavel.studentbsuby.workers.SyncUseCase
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {

    factory<Calendar> { Calendar.Base() }

    single<CaptchaRecognizer> { createCaptchaRecognizer() }

    single<ObservableSettings> { provideDefaultSettings() }

    single<ObservableSettings>(named("CredentialsPrefs")) {
        provideSecureSettings("github.sushkpavel.studentbsuby_credentials")
    }

    single<Logger> { DefaultLogger() }

    factory<ResourceManager> { ResourceManager.Base() }

    factory<Dispatchers> { DispatchersImpl(CoroutineJobManagerImpl()) }

    factory<ErrorHandler> { ErrorHandler.Log(get()) }

    factory<UsernameProvider> { UsernameProviderImpl(get(named("CredentialsPrefs"))) }

    single<PlatformInfo> { platformInfo() }

    factory { SyncUseCase(get(), get(), get(), get()) }
}
