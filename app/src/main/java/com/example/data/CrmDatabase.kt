package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Client::class,
        ClientService::class,
        PackageTemplate::class,
        Task::class,
        Bill::class,
        Payment::class,
        Expense::class,
        Settings::class,
        ActivityLog::class
    ],
    version = 2,
    exportSchema = false
)
abstract class CrmDatabase : RoomDatabase() {

    abstract fun crmDao(): CrmDao

    companion object {
        @Volatile
        private var INSTANCE: CrmDatabase? = null

        fun getDatabase(context: Context): CrmDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CrmDatabase::class.java,
                    "ab_graphics_crm_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
