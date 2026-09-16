package github.alexzhirkevich.studentbsuby.di

import android.content.Context
import androidx.work.WorkManager
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.SharedPreferencesSettings
import github.alexzhirkevich.studentbsuby.dao.appDatabaseBuilder
import github.alexzhirkevich.studentbsuby.dao.createAppDatabase
import github.alexzhirkevich.studentbsuby.resources.Res
import github.alexzhirkevich.studentbsuby.resources.notification_channel_description
import github.alexzhirkevich.studentbsuby.resources.updates
import github.alexzhirkevich.studentbsuby.services.firebase.AnalyticsReporter
import github.alexzhirkevich.studentbsuby.services.firebase.AnalyticsReporterImpl
import github.alexzhirkevich.studentbsuby.services.firebase.CrashReporter
import github.alexzhirkevich.studentbsuby.services.firebase.CrashReporterImpl
import github.alexzhirkevich.studentbsuby.services.firebase.RemoteConfigClient
import github.alexzhirkevich.studentbsuby.services.firebase.RemoteConfigClientImpl
import github.alexzhirkevich.studentbsuby.services.store.ReviewLauncher
import github.alexzhirkevich.studentbsuby.services.store.ReviewLauncherImpl
import github.alexzhirkevich.studentbsuby.services.store.UpdateLauncher
import github.alexzhirkevich.studentbsuby.services.store.UpdateLauncherImpl
import github.alexzhirkevich.studentbsuby.util.ConnectivityManager
import github.alexzhirkevich.studentbsuby.util.InternetConnectivityManager
import github.alexzhirkevich.studentbsuby.util.NotificationCreator
import github.alexzhirkevich.studentbsuby.util.NotificationCreatorImpl
import github.alexzhirkevich.studentbsuby.util.PlatformActions
import github.alexzhirkevich.studentbsuby.util.PlatformActionsAndroid
import github.alexzhirkevich.studentbsuby.util.WorkerManager
import github.alexzhirkevich.studentbsuby.util.communication.StateFlowCommunication
import github.alexzhirkevich.studentbsuby.util.migrateLegacyCookies
import github.alexzhirkevich.studentbsuby.util.provideSecureSharedPreferences
import github.alexzhirkevich.studentbsuby.workers.SyncWorkerManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Android bindings of the platform-dependent parts of the graph.
 */
val platformModule = module {

    single { createAppDatabase(appDatabaseBuilder(androidContext())) }

    single<ObservableSettings>(named("CookiesPrefs")) {
        provideCookiesSettings(androidContext())
    }

    // One instance per process: every instance registers network callbacks that are
    // never unregistered, and a shared one already holds the current connectivity
    // when a screen starts observing it.
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
}

/**
 * Cookies preferences: the same encrypted file the original app used, with the legacy
 * okhttp cookie entries migrated to the JSON format BEFORE the preferences are wrapped
 * into Settings — existing sessions survive the update.
 */
private fun provideCookiesSettings(context: Context): ObservableSettings {
    val preferences = provideSecureSharedPreferences(context.packageName + "_cookies")
    migrateLegacyCookies(preferences)
    return SharedPreferencesSettings(preferences)
}
