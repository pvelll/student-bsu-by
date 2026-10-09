package github.sushkpavel.studentbsuby.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

val sharedModules: List<Module> = listOf(
    appModule,
    networkModule,
    databaseModule,
    loginModule,
    profileModule,
    subjectsModule,
    progressModule,
    timetableModule,
    newsModule,
    hostelModule,
    paidServicesModule,
    settingsModule,
    securityModule,
    aboutModule,
    mainModule,
)

fun initKoin(
    platformModules: List<Module> = emptyList(),
    config: KoinAppDeclaration? = null,
): KoinApplication = startKoin {
    config?.invoke(this)
    modules(platformModules + sharedModules)
}
