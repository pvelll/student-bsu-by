package github.sushkpavel.studentbsuby.di

import github.sushkpavel.studentbsuby.services.lock.AppLockManager
import org.koin.dsl.module

val securityModule = module {

    single { AppLockManager(get(), get(), get()) }
}
