package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity)
}

@Dao
interface BikeDao {
    @Query("SELECT * FROM bikes WHERE userId = :userId")
    fun getBikesForUser(userId: String): Flow<List<BikeEntity>>

    @Query("SELECT * FROM bikes WHERE id = :bikeId LIMIT 1")
    suspend fun getBikeById(bikeId: String): BikeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBike(bike: BikeEntity)

    @Update
    suspend fun updateBike(bike: BikeEntity)

    @Query("UPDATE bikes SET currentMileage = :mileage WHERE id = :bikeId")
    suspend fun updateBikeMileage(bikeId: String, mileage: Int)

    @Query("DELETE FROM bikes WHERE id = :bikeId")
    suspend fun deleteBike(bikeId: String)
}

@Dao
interface ServiceIntervalDao {
    @Query("SELECT * FROM service_intervals WHERE bikeId = :bikeId")
    fun getIntervalsForBike(bikeId: String): Flow<List<ServiceIntervalEntity>>

    @Query("SELECT * FROM service_intervals WHERE bikeId = :bikeId")
    suspend fun getIntervalsForBikeSync(bikeId: String): List<ServiceIntervalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntervals(intervals: List<ServiceIntervalEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInterval(interval: ServiceIntervalEntity)

    @Query("DELETE FROM service_intervals WHERE bikeId = :bikeId")
    suspend fun deleteIntervalsForBike(bikeId: String)
}

@Dao
interface ServiceHistoryDao {
    @Query("SELECT * FROM service_histories WHERE bikeId = :bikeId")
    fun getHistoryForBike(bikeId: String): Flow<List<ServiceHistoryEntity>>

    @Query("SELECT * FROM service_histories WHERE bikeId = :bikeId")
    suspend fun getHistoryForBikeSync(bikeId: String): List<ServiceHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryEntry(entry: ServiceHistoryEntity)

    @Query("DELETE FROM service_histories WHERE bikeId = :bikeId")
    suspend fun deleteHistoryForBike(bikeId: String)
}
