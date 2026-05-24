package com.example.data

import kotlinx.coroutines.flow.Flow
import java.util.UUID

class MotoTrackerRepository(private val db: AppDatabase) {

    private val userDao = db.userDao()
    private val bikeDao = db.bikeDao()
    private val intervalDao = db.serviceIntervalDao()
    private val historyDao = db.serviceHistoryDao()

    companion object {
        val DEFAULT_INTERVALS = listOf(
            "aceite" to ("Cambio de Aceite" to 3000),
            "frenos" to ("Revisión de Frenos" to 10000),
            "llantas" to ("Cambio de Llantas" to 15000),
            "cadena" to ("Lubricación de Cadena" to 1000)
        )
    }

    /// --- AUTHENTICATION ---

    suspend fun signUp(email: String, javaPasswordHash: String): Result<UserEntity> {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isEmpty()) return Result.failure(Exception("El correo no puede estar vacío"))
        if (javaPasswordHash.length < 4) return Result.failure(Exception("La contraseña debe ser de al menos 4 caracteres"))

        val existing = userDao.getUserByEmail(cleanEmail)
        if (existing != null) {
            return Result.failure(Exception("Este correo ya está registrado"))
        }

        val newUser = UserEntity(
            id = UUID.randomUUID().toString(),
            email = cleanEmail,
            password = javaPasswordHash
        )
        userDao.insertUser(newUser)
        return Result.success(newUser)
    }

    suspend fun login(email: String, javaPasswordHash: String): Result<UserEntity> {
        val cleanEmail = email.trim().lowercase()
        val user = userDao.getUserByEmail(cleanEmail)
        return if (user != null && user.password == javaPasswordHash) {
            Result.success(user)
        } else {
            Result.failure(Exception("Correo o contraseña incorrectos"))
        }
    }

    /// --- BIKES AND GARAGE ---

    fun getBikesForUser(userId: String): Flow<List<BikeEntity>> {
        return bikeDao.getBikesForUser(userId)
    }

    suspend fun getBike(bikeId: String): BikeEntity? {
        return bikeDao.getBikeById(bikeId)
    }

    suspend fun addBike(
        userId: String,
        brand: String,
        model: String,
        year: Int,
        style: String,
        photoUrl: String,
        currentMileage: Int,
        lastServiceKm: Int,
        lastServiceDate: String
    ): Result<String> {
        if (brand.trim().isEmpty() || model.trim().isEmpty()) {
            return Result.failure(Exception("La marca y el modelo son obligatorios"))
        }
        if (lastServiceKm > currentMileage) {
            return Result.failure(Exception("El último servicio no puede ser mayor al odómetro actual"))
        }

        val bikeId = UUID.randomUUID().toString()
        val newBike = BikeEntity(
            id = bikeId,
            userId = userId,
            brand = brand,
            model = model,
            year = year,
            style = style,
            photoUrl = photoUrl,
            currentMileage = currentMileage
        )

        // Save bike
        bikeDao.insertBike(newBike)

        // Insert default intervals
        val intervalEntities = DEFAULT_INTERVALS.map { (id, details) ->
            val (name, km) = details
            ServiceIntervalEntity(
                id = "${bikeId}_$id",
                bikeId = bikeId,
                intervalId = id,
                name = name,
                kmInterval = km
            )
        }
        intervalDao.insertIntervals(intervalEntities)

        // Create starting history logs baseline (Crucial Punto de Partida!)
        intervalEntities.forEach { interval ->
            val baseHistory = ServiceHistoryEntity(
                id = UUID.randomUUID().toString(),
                bikeId = bikeId,
                date = lastServiceDate,
                typeId = interval.intervalId,
                typeName = interval.name,
                mileage = lastServiceKm,
                cost = 0.0,
                notes = "Punto de partida configurado al registrar la moto"
            )
            historyDao.insertHistoryEntry(baseHistory)
        }

        return Result.success(bikeId)
    }

    suspend fun updateBikeMileage(bikeId: String, newMileage: Int): Result<Unit> {
        val bike = bikeDao.getBikeById(bikeId) ?: return Result.failure(Exception("Moto no encontrada"))
        if (newMileage < bike.currentMileage) {
            return Result.failure(Exception("El nuevo kilometraje no puede ser menor al actual"))
        }
        bikeDao.updateBikeMileage(bikeId, newMileage)
        return Result.success(Unit)
    }

    suspend fun addCustomInterval(bikeId: String, name: String, frequency: Int, lastKm: Int, lastDate: String): Result<Unit> {
        if (name.trim().isEmpty()) return Result.failure(Exception("El nombre del servicio no puede estar vacío"))
        if (frequency <= 0) return Result.failure(Exception("La frecuencia debe ser mayor a 0"))

        val intervalId = "custom_" + name.trim().lowercase().replace(Regex("[^a-z0-9]"), "_") + "_" + System.currentTimeMillis()
        val entity = ServiceIntervalEntity(
            id = "${bikeId}_$intervalId",
            bikeId = bikeId,
            intervalId = intervalId,
            name = name,
            kmInterval = frequency
        )
        intervalDao.insertInterval(entity)

        // Insert baseline historical entry for custom category
        val baseHistory = ServiceHistoryEntity(
            id = UUID.randomUUID().toString(),
            bikeId = bikeId,
            date = lastDate,
            typeId = intervalId,
            typeName = name,
            mileage = lastKm,
            cost = 0.0,
            notes = "Punto de partida configurado para el servicio personalizado"
        )
        historyDao.insertHistoryEntry(baseHistory)

        return Result.success(Unit)
    }

    fun getIntervalsForBike(bikeId: String): Flow<List<ServiceIntervalEntity>> {
        return intervalDao.getIntervalsForBike(bikeId)
    }

    suspend fun updateBikeIntervals(bikeId: String, updated: List<ServiceIntervalEntity>) {
        intervalDao.insertIntervals(updated)
    }

    fun getHistoryForBike(bikeId: String): Flow<List<ServiceHistoryEntity>> {
        return historyDao.getHistoryForBike(bikeId)
    }

    suspend fun addHistoryEntry(
        bikeId: String,
        date: String,
        typeId: String,
        typeName: String,
        mileage: Int,
        cost: Double,
        notes: String
    ): Result<Unit> {
        val bike = bikeDao.getBikeById(bikeId) ?: return Result.failure(Exception("Moto no encontrada"))
        
        val historyEntry = ServiceHistoryEntity(
            id = UUID.randomUUID().toString(),
            bikeId = bikeId,
            date = date,
            typeId = typeId,
            typeName = typeName,
            mileage = mileage,
            cost = cost,
            notes = notes
        )
        historyDao.insertHistoryEntry(historyEntry)

        // Sync bike odometer if the entry mileage is higher!
        if (mileage > bike.currentMileage) {
            bikeDao.updateBikeMileage(bikeId, mileage)
        }

        return Result.success(Unit)
    }

    suspend fun deleteBike(bikeId: String) {
        bikeDao.deleteBike(bikeId)
        intervalDao.deleteIntervalsForBike(bikeId)
        historyDao.deleteHistoryForBike(bikeId)
    }

    // Demo Seeding Data! Creates a user "demo@mototracker.com" and fields a clean Yamaha MT-07
    suspend fun setupDemoMode(): Result<UserEntity> {
        val demoEmail = "demo@mototracker.com"
        val existingUser = userDao.getUserByEmail(demoEmail)
        val user = if (existingUser == null) {
            val newUser = UserEntity(
                id = "demo_user_id",
                email = demoEmail,
                password = "demo"
            )
            userDao.insertUser(newUser)
            newUser
        } else {
            existingUser
        }

        // Wait, did demo already have bikes? 
        // Let's query sync or async. We can see if we need to insert. Let's just try to check if there are any bikes for the user.
        // We will insert one if none exist.
        return Result.success(user)
    }

    suspend fun seedDemoBikeIfUserHasNone(userId: String) {
        // Simple helper to populate beautifully
        val bikeId = "demo_bike_mt07"
        val existingBike = bikeDao.getBikeById(bikeId)
        if (existingBike == null) {
            val bike = BikeEntity(
                id = bikeId,
                userId = userId,
                brand = "Yamaha",
                model = "MT-07 (Demo)",
                year = 2023,
                style = "sport",
                photoUrl = "",
                currentMileage = 11950
            )
            bikeDao.insertBike(bike)

            // Intervals
            val intervalEntities = DEFAULT_INTERVALS.map { (id, details) ->
                val (name, km) = details
                ServiceIntervalEntity(
                    id = "${bikeId}_$id",
                    bikeId = bikeId,
                    intervalId = id,
                    name = name,
                    kmInterval = km
                )
            }
            intervalDao.insertIntervals(intervalEntities)

            // Let's seed initial base logs at 11000 km, 1 month ago
            val oneMonthAgo = "2026-04-24"
            intervalEntities.forEach { interval ->
                val baseHistory = ServiceHistoryEntity(
                    id = UUID.randomUUID().toString(),
                    bikeId = bikeId,
                    date = oneMonthAgo,
                    typeId = interval.intervalId,
                    typeName = interval.name,
                    mileage = 11000,
                    cost = 0.0,
                    notes = "Punto de partida configurado al registrar la moto"
                )
                historyDao.insertHistoryEntry(baseHistory)
            }

            // Let's log some details: Chain lubrication at 11,800 km, cost $15
            val chainLub = ServiceHistoryEntity(
                id = UUID.randomUUID().toString(),
                bikeId = bikeId,
                date = "2026-05-15",
                typeId = "cadena",
                typeName = "Lubricación de Cadena",
                mileage = 11800,
                cost = 15.0,
                notes = "Limpieza con querosén y lubricación con grasa sintética Ipone."
            )
            historyDao.insertHistoryEntry(chainLub)

            // Let's log old service: Oil change at 9,000 km, cost $65
            val oldOil = ServiceHistoryEntity(
                id = UUID.randomUUID().toString(),
                bikeId = bikeId,
                date = "2026-03-10",
                typeId = "aceite",
                typeName = "Cambio de Aceite",
                mileage = 9000,
                cost = 65.0,
                notes = "Filtro K&N y aceite semisintético Yamalube 10W-40."
            )
            historyDao.insertHistoryEntry(oldOil)
        }
    }
}
