package github.sushkpavel.studentbsuby.di

import android.content.Context
import androidx.work.WorkManager
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.SharedPreferencesSettings
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
import github.sushkpavel.studentbsuby.util.PlatformActionsAndroid
import github.sushkpavel.studentbsuby.util.WorkerManager
import github.sushkpavel.studentbsuby.util.communication.StateFlowCommunication
import github.sushkpavel.studentbsuby.util.migrateLegacyCookies
import github.sushkpavel.studentbsuby.util.provideSecureSharedPreferences
import github.sushkpavel.studentbsuby.workers.SyncWorkerManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val platformModule = module {

    single { createAppDatabase(appDatabaseBuilder(androidContext())) }

    single<ObservableSettings>(named("CookiesPrefs")) {
        provideCookiesSettings(androidContext())
    }

    single<ConnectivityManager> {
        InternetConnectivityManager(androidContext(), StateFlowCommunication(false))
    }

    single<WorkerManager> {
        SyncWorkerManager(WorkManager.getInstance(androidContext()))
    }

    factory<NotificationCreator> {
        NotificationCreatorImpl(
            context = androidContext(),
            channelId = "CHANNEL_SYNCHRONIZATION",
            channelName = Res.string.updates,
            channelDescription = Res.string.notification_channel_description,
        )
    }

    single<AnalyticsReporter> { AnalyticsReporterImpl(androidContext()) }

    single<CrashReporter> { CrashReporterImpl() }

    single<RemoteConfigClient> { RemoteConfigClientImpl() }

    single<ReviewLauncher> { ReviewLauncherImpl(androidContext()) }

    single<UpdateLauncher> { UpdateLauncherImpl(androidContext()) }

    single<PlatformActions> { PlatformActionsAndroid(androidContext()) }

    single<BiometricAuthenticator> { BiometricAuthenticatorImpl(androidContext()) }
}

private fun provideCookiesSettings(context: Context): ObservableSettings {
    val preferences = provideSecureSharedPreferences(context.packageName + "_cookies")
    migrateLegacyCookies(preferences)
    return SharedPreferencesSettings(preferences)
}
