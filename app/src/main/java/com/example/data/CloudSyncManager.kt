package com.example.data

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CloudSyncManager {
    private const val TAG = "CloudSyncManager"
    private const val BUCKET_URL = "https://kvdb.io/A9S7Aqy7X6uFpbe69B6e72/" // Unique public bucket for this app

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .build()

    private val adapter = moshi.adapter(CloudSyncPayload::class.java)

    private fun getSecureKey(email: String): String {
        val cleanEmail = email.trim().lowercase()
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(cleanEmail.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { String.format("%02x", it) }
        } catch (e: Exception) {
            // Fallback hex
            cleanEmail.toByteArray(Charsets.UTF_8).joinToString("") { String.format("%02x", it) }
        }
    }

    suspend fun backupData(user: UserEntity, db: AppDatabase): Boolean = withContext(Dispatchers.IO) {
        val email = user.email
        val key = getSecureKey(email)
        val url = "$BUCKET_URL$key"

        try {
            val userId = user.id
            val bikes = db.bikeDao().getBikesForUserSync(userId)
            val intervalsList = mutableListOf<ServiceIntervalEntity>()
            val historiesList = mutableListOf<ServiceHistoryEntity>()
            val fuelLogsList = mutableListOf<FuelLogEntity>()

            bikes.forEach { bike ->
                intervalsList.addAll(db.serviceIntervalDao().getIntervalsForBikeSync(bike.id))
                historiesList.addAll(db.serviceHistoryDao().getHistoryForBikeSync(bike.id))
                fuelLogsList.addAll(db.fuelLogDao().getFuelLogsForBikeSync(bike.id))
            }

            val customStyles = db.customBikeStyleDao().getCustomStylesForUserSync(userId)

            val payload = CloudSyncPayload(
                user = user,
                bikes = bikes,
                intervals = intervalsList,
                histories = historiesList,
                customStyles = customStyles,
                fuelLogs = fuelLogsList
            )

            val json = adapter.toJson(payload)
            val requestBody = json.toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(url)
                .put(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "Sincronización en la nube exitosa para $email")
                    true
                } else {
                    Log.e(TAG, "Error de sincronización: ${response.code} / ${response.message}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fallo de respaldo en la nube", e)
            false
        }
    }

    suspend fun restoreDataAndMerge(email: String, db: AppDatabase): UserEntity? = withContext(Dispatchers.IO) {
        val key = getSecureKey(email)
        val url = "$BUCKET_URL$key"

        try {
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.d(TAG, "No se encontró respaldo en la nube para $email (Status: ${response.code})")
                    return@withContext null
                }

                val json = response.body?.string() ?: return@withContext null
                val payload = adapter.fromJson(json) ?: return@withContext null

                // Insert user
                db.userDao().insertUser(payload.user)

                // Insert Custom Styles
                payload.customStyles.forEach { style ->
                    db.customBikeStyleDao().insertCustomStyle(style)
                }

                // Insert Bikes
                payload.bikes.forEach { bike ->
                    db.bikeDao().insertBike(bike)
                }

                // Insert intervals
                db.serviceIntervalDao().insertIntervals(payload.intervals)

                // Insert service histories
                payload.histories.forEach { history ->
                    db.serviceHistoryDao().insertHistoryEntry(history)
                }

                // Insert fuel logs
                payload.fuelLogs.forEach { log ->
                    db.fuelLogDao().insertFuelLog(log)
                }

                Log.d(TAG, "Datos de la nube restaurados exitosamente para $email")
                payload.user
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al restaurar desde la nube", e)
            null
        }
    }
}
