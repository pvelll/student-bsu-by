package github.sushkpavel.studentbsuby.di

import github.sushkpavel.studentbsuby.demo.DemoAccount
import github.sushkpavel.studentbsuby.demo.DemoMode
import org.koin.core.qualifier.named
import org.koin.dsl.module

val demoModule = module {

    single { DemoMode(get(named("CredentialsPrefs"))) }

    factory { DemoAccount() }
}
