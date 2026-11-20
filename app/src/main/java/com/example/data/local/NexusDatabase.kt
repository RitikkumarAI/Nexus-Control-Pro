package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SessionHistoryEntity::class,
        AppUsageEntity::class,
        PerformanceMetricEntity::class,
        SyncQueueEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class NexusDatabase : RoomDatabase() {

    abstract fun sessionHistoryDao(): SessionHistoryDao
    abstract fun sessionDao(): SessionDao
    abstract fun appUsageDao(): AppUsageDao
    abstract fun performanceDao(): PerformanceDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: NexusDatabase? = null

        fun getDatabase(context: Context): NexusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NexusDatabase::class.java,
                    "nexus_control_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
