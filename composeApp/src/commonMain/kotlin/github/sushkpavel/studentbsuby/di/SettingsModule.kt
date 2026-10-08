package github.sushkpavel.studentbsuby.di

import github.sushkpavel.studentbsuby.repo.SettingsRepository
import github.sushkpavel.studentbsuby.ui.screens.settings.SettingsEventHandlerImpl
import github.sushkpavel.studentbsuby.ui.screens.settings.SettingsState
import github.sushkpavel.studentbsuby.ui.screens.settings.SettingsViewModel
import github.sushkpavel.studentbsuby.util.communication.StateFlowCommunication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Mirrors the original SettingsModule (ViewModelComponent).
 */
val settingsModule = module {

    // Unqualified ObservableSettings resolves the default preferences file;
    // AnalyticsReporter/CrashReporter/WorkerManager come from the platform modules.
    factory { SettingsRepository(get(), get(), get(), get()) }

    viewModel {
        val repo = get<SettingsRepository>()
        val stateCommunication = StateFlowCommunication(
            initial = SettingsState(
                notificationsEnabled = repo.synchronizationEnabled,
                collectStatistic = repo.collectStatistics,
                collectCrashlytics = repo.collectCrashlytics
            )
        )
        val eventHandler = SettingsEventHandlerImpl(
            settingsRepository = repo,
            mapper = stateCommunication,
            logger = get(),
            platformActions = get()
        )
        SettingsViewModel(
            state = stateCommunication,
            handler = eventHandler
        )
    }
}
