package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        BikeEntity::class,
        ServiceIntervalEntity::class,
        ServiceHistoryEntity::class,
        CustomBikeStyleEntity::class,
        FuelLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun bikeDao(): BikeDao
    abstract fun serviceIntervalDao(): ServiceIntervalDao
    abstract fun serviceHistoryDao(): ServiceHistoryDao
    abstract fun customBikeStyleDao(): CustomBikeStyleDao
    abstract fun fuelLogDao(): FuelLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mototracker_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
