package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MotoTrackerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MotoTrackerRepository
    private val prefs = application.getSharedPreferences("mototracker_prefs", Context.MODE_PRIVATE)

    // User preferences (persistent)
    private val _odometerUnit = MutableStateFlow(prefs.getString("odometer_unit", "km") ?: "km")
    val odometerUnit: StateFlow<String> = _odometerUnit.asStateFlow()

    private val _currencySymbol = MutableStateFlow(prefs.getString("currency_symbol", "$") ?: "$")
    val currencySymbol: StateFlow<String> = _currencySymbol.asStateFlow()

    private val _dailyAverageKm = MutableStateFlow(prefs.getFloat("daily_average", 15f))
    val dailyAverageKm: StateFlow<Float> = _dailyAverageKm.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = MotoTrackerRepository(database)

        // Persistent auto-login on app start
        val savedEmail = prefs.getString("logged_user_email", null)
        if (savedEmail != null) {
            viewModelScope.launch {
                var user = repository.getUserByEmail(savedEmail)
                if (user == null) {
                    // Pull from cloud if database was cleared but email is saved
                    user = CloudSyncManager.restoreDataAndMerge(savedEmail, database)
                } else {
                    // If user already exists locally, run a fast background merge to get latest remote edits
                    CloudSyncManager.restoreDataAndMerge(savedEmail, database)
                }
                if (user != null) {
                    _currentUser.value = user
                    val savedBikeId = prefs.getString("selected_bike_id", null)
                    _activeBikeId.value = savedBikeId
                }
            }
        }
    }

    fun updateOdometerUnit(unit: String) {
        _odometerUnit.value = unit
        prefs.edit().putString("odometer_unit", unit).apply()
    }

    fun updateCurrencySymbol(symbol: String) {
        _currencySymbol.value = symbol
        prefs.edit().putString("currency_symbol", symbol).apply()
    }

    fun updateDailyAverageKm(avg: Float) {
        _dailyAverageKm.value = avg
        prefs.edit().putFloat("daily_average", avg).apply()
    }

    // Auth State
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Active Bike Selection
    private val _activeBikeId = MutableStateFlow<String?>(null)
    val activeBikeId: StateFlow<String?> = _activeBikeId.asStateFlow()

    // Theme Mode Selection
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // Dash Tab Routing State
    private val _activeTab = MutableStateFlow("inicio") // "inicio", "historial", "servicio", "ajustes", "combustible"
    val activeTab: StateFlow<String> = _activeTab.asStateFlow()

    // View Style Gauge Mode
    private val _dashboardViewMode = MutableStateFlow("list") // "list", "rpm"
    val dashboardViewMode: StateFlow<String> = _dashboardViewMode.asStateFlow()

    // Feedback State
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _toastIsError = MutableStateFlow(false)
    val toastIsError: StateFlow<Boolean> = _toastIsError.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Reactive Collections
    @OptIn(ExperimentalCoroutinesApi::class)
    val bikesList: StateFlow<List<BikeEntity>> = _currentUser
        .filterNotNull()
        .flatMapLatest { user ->
            repository.getBikesForUser(user.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeBike: StateFlow<BikeEntity?> = _activeBikeId
        .flatMapLatest { bikeId ->
            if (bikeId == null) flowOf<BikeEntity?>(null) else repository.getBikeFlow(bikeId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeBikeIntervals: StateFlow<List<ServiceIntervalEntity>> = _activeBikeId
        .flatMapLatest { bikeId ->
            if (bikeId == null) flowOf<List<ServiceIntervalEntity>>(emptyList()) else repository.getIntervalsForBike(bikeId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeBikeHistory: StateFlow<List<ServiceHistoryEntity>> = _activeBikeId
        .flatMapLatest { bikeId ->
            if (bikeId == null) flowOf<List<ServiceHistoryEntity>>(emptyList()) else repository.getHistoryForBike(bikeId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHistoryList: StateFlow<List<ServiceHistoryEntity>> = repository.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeBikeFuelLogs: StateFlow<List<FuelLogEntity>> = _activeBikeId
        .flatMapLatest { bikeId ->
            if (bikeId == null) flowOf<List<FuelLogEntity>>(emptyList()) else repository.getFuelLogsForBike(bikeId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val customStylesList: StateFlow<List<CustomBikeStyleEntity>> = _currentUser
        .filterNotNull()
        .flatMapLatest { user ->
            repository.getCustomStyles(user.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Actions implementation

    fun showToast(msg: String, isError: Boolean = false) {
        _toastMessage.value = msg
        _toastIsError.value = isError
    }

    fun dismissToast() {
        _toastMessage.value = null
    }

    fun setDashboardViewMode(mode: String) {
        _dashboardViewMode.value = mode
    }

    fun setActiveTab(tab: String) {
        _activeTab.value = tab
    }

    fun selectBike(bikeId: String?) {
        _activeBikeId.value = bikeId
        _activeTab.value = "inicio"
        if (bikeId != null) {
            prefs.edit().putString("selected_bike_id", bikeId).apply()
        } else {
            prefs.edit().remove("selected_bike_id").apply()
        }
    }

    private fun triggerCloudBackup() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val database = AppDatabase.getDatabase(getApplication())
            CloudSyncManager.backupData(user, database)
        }
    }

    fun login(email: String, javaPasswordHash: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.login(email, javaPasswordHash)
            _isLoading.value = false
            result.onSuccess { user ->
                _currentUser.value = user
                prefs.edit().putString("logged_user_email", user.email).apply()
                showToast("Sesión iniciada con éxito")
                onSuccess()
                // Sync data from cloud upon successful manual login on another device
                val database = AppDatabase.getDatabase(getApplication())
                CloudSyncManager.restoreDataAndMerge(user.email, database)
            }.onFailure { err ->
                showToast(err.message ?: "Error al ingresar", isError = true)
            }
        }
    }

    fun signUp(email: String, javaPasswordHash: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.signUp(email, javaPasswordHash)
            _isLoading.value = false
            result.onSuccess { user ->
                _currentUser.value = user
                prefs.edit().putString("logged_user_email", user.email).apply()
                showToast("Cuenta creada e iniciada")
                onSuccess()
                // Initial backup
                triggerCloudBackup()
            }.onFailure { err ->
                showToast(err.message ?: "Error al registrarse", isError = true)
            }
        }
    }

    fun signInWithGoogle(email: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.signUpOrLoginGoogle(email)
            _isLoading.value = false
            result.onSuccess { user ->
                _currentUser.value = user
                prefs.edit().putString("logged_user_email", user.email).apply()
                showToast("Cuenta de Google vinculada correctamente")
                onSuccess()
                // Initial sync check
                triggerCloudBackup()
            }.onFailure { err ->
                showToast(err.message ?: "Error al conectar Google", isError = true)
            }
        }
    }

    fun setupDemoMode(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.setupDemoMode()
            _isLoading.value = false
            result.onSuccess { user ->
                _currentUser.value = user
                repository.seedDemoBikeIfUserHasNone(user.id)
                prefs.edit().putString("logged_user_email", user.email).apply()
                showToast("Modo de prueba activado")
                onSuccess()
            }.onFailure { err ->
                showToast(err.message ?: "Error en demo", isError = true)
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _activeBikeId.value = null
        _activeTab.value = "inicio"
        prefs.edit().remove("logged_user_email").remove("selected_bike_id").apply()
        showToast("Sesión cerrada")
    }

    fun addBike(
        brand: String,
        model: String,
        year: Int,
        style: String,
        photoUrl: String,
        currentMileage: Int,
        lastServiceKm: Int,
        lastServiceDate: String,
        onSuccess: () -> Unit
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.addBike(
                userId = user.id,
                brand = brand,
                model = model,
                year = year,
                style = style,
                photoUrl = photoUrl,
                currentMileage = currentMileage,
                lastServiceKm = lastServiceKm,
                lastServiceDate = lastServiceDate
            )
            _isLoading.value = false
            result.onSuccess { bikeId ->
                _activeBikeId.value = bikeId
                showToast("Motocicleta agregada correctamente")
                onSuccess()
                triggerCloudBackup()
            }.onFailure { err ->
                showToast(err.message ?: "Error al guardar", isError = true)
            }
        }
    }

    fun updateOdometer(bikeId: String, newMileage: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.updateBikeMileage(bikeId, newMileage)
            _isLoading.value = false
            result.onSuccess {
                showToast("Odómetro actualizado")
                // Re-select to force re-fetch
                _activeBikeId.value = bikeId
                triggerCloudBackup()
            }.onFailure { err ->
                showToast(err.message ?: "Error de odómetro", isError = true)
            }
        }
    }

    fun updateBikeDetails(bikeId: String, brand: String, model: String, year: Int, style: String, photoUrl: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.updateBikeDetails(bikeId, brand, model, year, style, photoUrl)
            _isLoading.value = false
            result.onSuccess {
                showToast("Datos de la moto actualizados")
                // Re-select to force re-fetch
                _activeBikeId.value = bikeId
                triggerCloudBackup()
            }.onFailure { err ->
                showToast(err.message ?: "Error al actualizar", isError = true)
            }
        }
    }

    fun saveHistoryEntry(
        bikeId: String,
        date: String,
        typeId: String,
        typeName: String,
        mileage: Int,
        cost: Double,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.addHistoryEntry(bikeId, date, typeId, typeName, mileage, cost, notes)
            _isLoading.value = false
            result.onSuccess {
                showToast("Registro guardado")
                _activeBikeId.value = bikeId // refresh details
                _activeTab.value = "inicio"
                onSuccess()
                triggerCloudBackup()
            }.onFailure { err ->
                showToast(err.message ?: "Error al registrar", isError = true)
            }
        }
    }

    fun addCustomInterval(bikeId: String, name: String, frequency: Int, lastKm: Int, lastDate: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.addCustomInterval(bikeId, name, frequency, lastKm, lastDate)
            _isLoading.value = false
            result.onSuccess {
                showToast("Servicio personalizado creado")
                _activeBikeId.value = bikeId // force refresh
                onSuccess()
                triggerCloudBackup()
            }.onFailure { err ->
                showToast(err.message ?: "Error", isError = true)
            }
        }
    }

    fun updateBikeIntervals(bikeId: String, updated: List<ServiceIntervalEntity>) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateBikeIntervals(bikeId, updated)
            _isLoading.value = false
            showToast("Frecuencias actualizadas")
            _activeBikeId.value = bikeId // force refresh
            triggerCloudBackup()
        }
    }

    fun deleteBike(bikeId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.deleteBike(bikeId)
            _isLoading.value = false
            showToast("Motocicleta eliminada")
            selectBike(null)
            onSuccess()
            triggerCloudBackup()
        }
    }

    fun toggleTheme() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun addCustomStyle(name: String, iconKey: String, onSuccess: () -> Unit) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.addCustomStyle(user.id, name, iconKey)
            _isLoading.value = false
            result.onSuccess {
                showToast("Tipo de moto '$name' creado")
                onSuccess()
                triggerCloudBackup()
            }.onFailure { err ->
                showToast(err.message ?: "Error", isError = true)
            }
        }
    }

    fun addFuelLog(
        date: String,
        mileage: Int,
        liters: Double,
        cost: Double,
        notes: String,
        onSuccess: () -> Unit
    ) {
        val bikeId = _activeBikeId.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.addFuelLog(bikeId, date, mileage, liters, cost, notes)
            _isLoading.value = false
            result.onSuccess {
                showToast("Carga de combustible registrada")
                _activeBikeId.value = bikeId // force refresh
                onSuccess()
                triggerCloudBackup()
            }.onFailure { err ->
                showToast(err.message ?: "Error al registrar", isError = true)
            }
        }
    }

    fun deleteFuelLog(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.deleteFuelLog(id)
            _isLoading.value = false
            result.onSuccess {
                showToast("Registro borrado")
                _activeBikeId.value = _activeBikeId.value // force refresh
                triggerCloudBackup()
            }.onFailure { err ->
                showToast(err.message ?: "Error", isError = true)
            }
        }
    }
}
