package github.sushkpavel.studentbsuby

import android.app.Application
import android.content.Context
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import github.sushkpavel.studentbsuby.di.initKoin
import github.sushkpavel.studentbsuby.di.platformModule
import github.sushkpavel.studentbsuby.services.firebase.initFirebase
import github.sushkpavel.studentbsuby.util.AndroidAppContext
import github.sushkpavel.studentbsuby.workers.SyncWorker
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class MainApplication : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()
        // Must be set before Koin starts: settings providers, platformInfo() and
        // FileLogger read it.
        AndroidAppContext.context = this
        initFirebase(this)
        initKoin(platformModules = listOf(platformModule)) {
            androidLogger()
            androidContext(this@MainApplication)
        }
    }

    // On-demand WorkManager initialization (the default WorkManagerInitializer is
    // removed in the manifest); replaces the original HiltWorkerFactory so the
    // already-enqueued "SynchronizationWorker" periodic work keeps resolving.
    override val workManagerConfiguration: Configuration by lazy {
        Configuration.Builder()
            .setWorkerFactory(SyncWorkerFactory())
            .build()
    }
}

private class SyncWorkerFactory : WorkerFactory(), KoinComponent {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? = when (workerClassName) {
        SyncWorker::class.qualifiedName ->
            SyncWorker(appContext, workerParameters, get())
        else -> null
    }
}
