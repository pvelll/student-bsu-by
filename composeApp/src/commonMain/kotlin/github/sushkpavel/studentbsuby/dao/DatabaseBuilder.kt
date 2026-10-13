package github.sushkpavel.studentbsuby.dao

import androidx.room.RoomDatabase

internal const val APP_DATABASE_NAME = "db_cache"

fun createAppDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase =
    builder
        .addMigrations(MIGRATION_1_2)
        .fallbackToDestructiveMigration(dropAllTables = false)
        .build()
