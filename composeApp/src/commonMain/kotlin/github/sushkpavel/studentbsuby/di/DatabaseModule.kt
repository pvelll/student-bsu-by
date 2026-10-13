package github.sushkpavel.studentbsuby.di

import github.sushkpavel.studentbsuby.dao.AppDatabase
import org.koin.dsl.module

val databaseModule = module {

    single { get<AppDatabase>().userDao() }

    single { get<AppDatabase>().subjectsDao() }

    single { get<AppDatabase>().lessonsDao() }

    single { get<AppDatabase>().hostelDao() }

    single { get<AppDatabase>().paidServicesDao() }

    single { get<AppDatabase>().newsDao() }
}
