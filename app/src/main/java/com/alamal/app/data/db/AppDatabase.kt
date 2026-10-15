package com.alamal.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.alamal.app.data.db.dao.BreakDao
import com.alamal.app.data.db.dao.CreditDao
import com.alamal.app.data.db.dao.PeriodDao
import com.alamal.app.data.db.dao.RemoteWorkDao
import com.alamal.app.data.db.dao.WorkDayDao
import com.alamal.app.data.db.entity.BreakEntity
import com.alamal.app.data.db.entity.CreditEntity
import com.alamal.app.data.db.entity.PeriodEntity
import com.alamal.app.data.db.entity.RemoteWorkEntity
import com.alamal.app.data.db.entity.WorkDayEntity

@Database(
    entities = [
        WorkDayEntity::class,
        BreakEntity::class,
        RemoteWorkEntity::class,
        CreditEntity::class,
        PeriodEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workDayDao(): WorkDayDao
    abstract fun breakDao(): BreakDao
    abstract fun remoteWorkDao(): RemoteWorkDao
    abstract fun creditDao(): CreditDao
    abstract fun periodDao(): PeriodDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // IMPORTANT — migrations
        // ----------------------
        // The database is currently at version 1 and there are no migrations
        // defined yet. Two things to keep in mind for future releases:
        //
        // 1. Before bumping `version` to 2, write a Migration and pass it
        //    to `.addMigrations(MIGRATION_1_2)` below. Otherwise Room will
        //    throw IllegalStateException on devices that already have v1,
        //    crashing the app on update.
        //
        // 2. `fallbackToDestructiveMigration()` is included NOW because the
        //    app has not been published yet — it lets the schema evolve
        //    during development without writing a migration for every
        //    change. It WIPES the database on version bump. Before the
        //    first public release:
        //      a) Remove `fallbackToDestructiveMigration()`.
        //      b) Add explicit migrations for every version bump.
        //
        // If you ever need to ship a v2 without writing a migration (e.g.
        // during beta), keep the fallback call — but document it loudly in
        // the release notes, and provide a backup/restore path so users
        // don't lose data.
        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "alamal.db",
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
