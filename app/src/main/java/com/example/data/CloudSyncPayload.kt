package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CloudSyncPayload(
    val user: UserEntity,
    val bikes: List<BikeEntity>,
    val intervals: List<ServiceIntervalEntity>,
    val histories: List<ServiceHistoryEntity>,
    val customStyles: List<CustomBikeStyleEntity>,
    val fuelLogs: List<FuelLogEntity>
)
