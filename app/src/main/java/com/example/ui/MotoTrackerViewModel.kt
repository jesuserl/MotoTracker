package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MotoTrackerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MotoTrackerRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = MotoTrackerRepository(database)
    }

    // Auth State
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Active Bike Selection
    private val _activeBikeId = MutableStateFlow<String?>(null)
    val activeBikeId: StateFlow<String?> = _activeBikeId.asStateFlow()

    // Dash Tab Routing State
    private val _activeTab = MutableStateFlow("inicio") // "inicio", "historial", "servicio", "ajustes"
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
        .filterNotNull()
        .flatMapLatest { bikeId ->
            flow { emit(repository.getBike(bikeId)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeBikeIntervals: StateFlow<List<ServiceIntervalEntity>> = _activeBikeId
        .filterNotNull()
        .flatMapLatest { bikeId ->
            repository.getIntervalsForBike(bikeId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeBikeHistory: StateFlow<List<ServiceHistoryEntity>> = _activeBikeId
        .filterNotNull()
        .flatMapLatest { bikeId ->
            repository.getHistoryForBike(bikeId)
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
    }

    fun login(email: String, javaPasswordHash: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.login(email, javaPasswordHash)
            _isLoading.value = false
            result.onSuccess { user ->
                _currentUser.value = user
                showToast("Sesión iniciada con éxito")
                onSuccess()
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
                showToast("Cuenta creada e iniciada")
                onSuccess()
            }.onFailure { err ->
                showToast(err.message ?: "Error al registrarse", isError = true)
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
            result.onSuccess {
                showToast("Motocicleta agregada correctamente")
                onSuccess()
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
            }.onFailure { err ->
                showToast(err.message ?: "Error de odómetro", isError = true)
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
        }
    }
}
