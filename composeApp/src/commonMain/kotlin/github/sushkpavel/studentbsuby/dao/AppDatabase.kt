package github.sushkpavel.studentbsuby.dao

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import github.sushkpavel.studentbsuby.data.models.*

@Database(
    entities = [
    User::class,
    Subject::class,
    Lesson::class,
    HostelAdvert::class,
    PaidServicesInfo::class,
    Bill::class,
    TuitionFeePayment::class,
    Receipt::class,
    News::class,
    NewsContent::class
               ],
    exportSchema = false,
    version = 2)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UsersDao

    abstract fun subjectsDao(): SubjectsDao

    abstract fun lessonsDao(): LessonsDao

    abstract fun hostelDao(): HostelDao

    abstract fun paidServicesDao(): PaidServicesDao

    abstract fun newsDao() : NewsDao
}

internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `Lesson` ADD COLUMN `date` TEXT NOT NULL DEFAULT ''")
    }
}

// The Room compiler generates the `actual` implementations.
@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
