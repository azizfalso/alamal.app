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

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "alamal.db",
                ).build().also { INSTANCE = it }
            }
    }
}
