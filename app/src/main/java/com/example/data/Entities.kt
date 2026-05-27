package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "users")
@JsonClass(generateAdapter = true)
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val password: String
)

@Entity(tableName = "bikes")
@JsonClass(generateAdapter = true)
data class BikeEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val brand: String,
    val model: String,
    val year: Int,
    val style: String,
    val photoUrl: String,
    val currentMileage: Int
)

@Entity(tableName = "service_intervals")
@JsonClass(generateAdapter = true)
data class ServiceIntervalEntity(
    @PrimaryKey val id: String, // format: "bikeId_intervalId"
    val bikeId: String,
    val intervalId: String,
    val name: String,
    val kmInterval: Int
)

@Entity(tableName = "service_histories")
@JsonClass(generateAdapter = true)
data class ServiceHistoryEntity(
    @PrimaryKey val id: String,
    val bikeId: String,
    val date: String,
    val typeId: String,
    val typeName: String,
    val mileage: Int,
    val cost: Double,
    val notes: String
)

@Entity(tableName = "custom_bike_styles")
@JsonClass(generateAdapter = true)
data class CustomBikeStyleEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val iconKey: String // sport, scooter, adventure, cruiser, scrambler, tracker, cafe_racer, custom
)

@Entity(tableName = "fuel_logs")
@JsonClass(generateAdapter = true)
data class FuelLogEntity(
    @PrimaryKey val id: String,
    val bikeId: String,
    val date: String,
    val mileage: Int,
    val liters: Double,
    val cost: Double,
    val notes: String
)
