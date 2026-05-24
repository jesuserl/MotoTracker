package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val password: String
)

@Entity(tableName = "bikes")
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
data class ServiceIntervalEntity(
    @PrimaryKey val id: String, // format: "bikeId_intervalId"
    val bikeId: String,
    val intervalId: String,
    val name: String,
    val kmInterval: Int
)

@Entity(tableName = "service_histories")
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
