package github.sushkpavel.studentbsuby.di

import com.russhwolf.settings.ObservableSettings
import github.sushkpavel.studentbsuby.dao.appDatabaseBuilder
import github.sushkpavel.studentbsuby.dao.createAppDatabase
import github.sushkpavel.studentbsuby.resources.Res
import github.sushkpavel.studentbsuby.resources.notification_channel_description
import github.sushkpavel.studentbsuby.resources.updates
import github.sushkpavel.studentbsuby.services.firebase.AnalyticsReporter
import github.sushkpavel.studentbsuby.services.firebase.AnalyticsReporterImpl
import github.sushkpavel.studentbsuby.services.firebase.CrashReporter
import github.sushkpavel.studentbsuby.services.firebase.CrashReporterImpl
import github.sushkpavel.studentbsuby.services.firebase.RemoteConfigClient
import github.sushkpavel.studentbsuby.services.firebase.RemoteConfigClientImpl
import github.sushkpavel.studentbsuby.services.lock.BiometricAuthenticator
import github.sushkpavel.studentbsuby.services.lock.BiometricAuthenticatorImpl
import github.sushkpavel.studentbsuby.services.store.ReviewLauncher
import github.sushkpavel.studentbsuby.services.store.ReviewLauncherImpl
import github.sushkpavel.studentbsuby.services.store.UpdateLauncher
import github.sushkpavel.studentbsuby.services.store.UpdateLauncherImpl
import github.sushkpavel.studentbsuby.util.ConnectivityManager
import github.sushkpavel.studentbsuby.util.InternetConnectivityManager
import github.sushkpavel.studentbsuby.util.NotificationCreator
import github.sushkpavel.studentbsuby.util.NotificationCreatorImpl
import github.sushkpavel.studentbsuby.util.PlatformActions
import github.sushkpavel.studentbsuby.util.PlatformActionsIos
import github.sushkpavel.studentbsuby.util.WorkerManager
import github.sushkpavel.studentbsuby.util.communication.StateFlowCommunication
import github.sushkpavel.studentbsuby.util.provideSecureSettings
import github.sushkpavel.studentbsuby.workers.BackgroundSyncScheduler
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * iOS bindings of the platform-dependent parts of the graph.
 */
val platformModule = module {

    single { createAppDatabase(appDatabaseBuilder()) }

    single<ObservableSettings>(named("CookiesPrefs")) {
        provideSecureSettings("github.sushkpavel.studentbsuby_cookies")
    }

    // One instance per process (see the Android module).
    single<ConnectivityManager> {
        InternetConnectivityManager(StateFlowCommunication(false))
    }

    // Bound with the concrete type too: MainViewController resolves
    // BackgroundSyncScheduler to register the BGTaskScheduler handler at startup.
    single { BackgroundSyncScheduler(get()) } bind WorkerManager::class

    factory<NotificationCreator> {
        NotificationCreatorImpl(
            channelId = "CHANNEL_SYNCHRONIZATION",
            channelName = Res.string.updates,
            channelDescription = Res.string.notification_channel_description,
        )
    }

    single<AnalyticsReporter> { AnalyticsReporterImpl() }

    single<CrashReporter> { CrashReporterImpl() }

    single<RemoteConfigClient> { RemoteConfigClientImpl() }

    single<ReviewLauncher> { ReviewLauncherImpl() }

    single<UpdateLauncher> { UpdateLauncherImpl() }

    single<PlatformActions> { PlatformActionsIos() }

    single<BiometricAuthenticator> { BiometricAuthenticatorImpl() }
}
