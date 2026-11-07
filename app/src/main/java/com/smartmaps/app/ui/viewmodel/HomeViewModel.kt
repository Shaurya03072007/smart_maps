package com.smartmaps.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartmaps.app.data.local.DatabaseSeeder
import com.smartmaps.app.data.local.dao.ServiceCategoryDao
import com.smartmaps.app.data.local.dao.ServiceDao
import com.smartmaps.app.data.local.entity.ServiceCategoryEntity
import com.smartmaps.app.data.local.entity.ServiceEntity
import com.smartmaps.app.data.location.LocationRepository
import com.smartmaps.app.data.remote.model.LatLng
import com.smartmaps.app.data.remote.model.Place
import com.smartmaps.app.data.remote.model.ProviderInfo
import com.smartmaps.app.data.repository.BookingRepository
import com.smartmaps.app.data.repository.GeminiRepository
import com.smartmaps.app.data.repository.PlaceRepository
import com.smartmaps.app.data.repository.ProviderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = false,
    val categories: List<ServiceCategoryEntity> = emptyList(),
    val popularServices: List<ServiceEntity> = emptyList(),
    val nearbyProviders: List<ProviderInfo> = emptyList(),
    val nearbyPlaces: List<Place> = emptyList(),
    val searchQuery: String = "",
    val currentLocation: LatLng? = null,
    val locationAddress: String = "",
    val error: String? = null,
    val isDbSeeded: Boolean = false,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val serviceCategoryDao: ServiceCategoryDao,
    private val serviceDao: ServiceDao,
    private val providerRepository: ProviderRepository,
    private val placeRepository: PlaceRepository,
    private val locationRepository: LocationRepository,
    private val geminiRepository: GeminiRepository,
    private val bookingRepository: BookingRepository,
    private val databaseSeeder: DatabaseSeeder
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        initialize()
    }

    private fun initialize() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Seed DB if empty
            databaseSeeder.seedIfEmpty()
            _uiState.update { it.copy(isDbSeeded = true) }

            // Eagerly sync providers from Supabase so registered providers appear immediately
            launch {
                providerRepository.syncProvidersWithSupabase()
            }

            // Load categories
            launch {
                serviceCategoryDao.getAllCategories().collect { cats ->
                    _uiState.update { it.copy(categories = cats) }
                }
            }

            // Load popular services (first 8)
            launch {
                serviceDao.getAllServices().collect { services ->
                    _uiState.update { it.copy(popularServices = services.take(8)) }
                }
            }

            // Load initial nearby data immediately using last known or default center so Home & Map are never empty
            val initialLatLng = locationRepository.currentLocation.value ?: LatLng(17.3850, 78.4867)
            _uiState.update { it.copy(currentLocation = initialLatLng, locationAddress = "SmartMaps Local Zone") }
            loadNearbyData(initialLatLng)

            // Observe the singleton StateFlow — updates arrive from
            // startLocationUpdates() or getLastKnownLocation() calls below
            launch {
                locationRepository.currentLocation.filterNotNull().collect { latLng ->
                    _uiState.update { it.copy(currentLocation = latLng, isLoading = false) }
                    if (locationRepository.hasMovedSignificantly(latLng)) {
                        locationRepository.markGeocodedAt(latLng)
                        loadNearbyData(latLng)
                        loadLocationAddress(latLng)
                    }
                }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * Called by LocationPermissionWrapper once the user grants permission.
     * Must use THIS ViewModel's injected locationRepository (the Hilt singleton)
     * so that currentLocation updates are visible to all consumers.
     */
    fun onLocationPermissionGranted() {
        viewModelScope.launch {
            // Get last cached location immediately so UI updates without waiting
            locationRepository.getLastKnownLocation()
        }
        viewModelScope.launch {
            // Then start continuous updates
            locationRepository.startLocationUpdates().collect { /* StateFlow updated inside */ }
        }
    }

    private fun loadNearbyData(latLng: LatLng) {
        viewModelScope.launch {
            // 1. Immediately load from local DB for fast display
            providerRepository.searchNearbyProviders(latLng, radiusKm = 10.0, limit = 10)
                .onSuccess { providers ->
                    _uiState.update { it.copy(nearbyProviders = providers) }
                }

            // 2. Nearby places (restaurants as default)
            placeRepository.searchNearbyPlaces(latLng, listOf("food"), radiusMeters = 3000, limit = 10)
                .onSuccess { places ->
                    _uiState.update { it.copy(nearbyPlaces = places) }
                }
        }

        // 3. Sync with Supabase in background, then refresh providers list silently
        viewModelScope.launch {
            val syncResult = providerRepository.syncProvidersWithSupabase()
            if (syncResult.isSuccess) {
                providerRepository.searchNearbyProviders(latLng, radiusKm = 10.0, limit = 10)
                    .onSuccess { providers ->
                        _uiState.update { it.copy(nearbyProviders = providers) }
                    }
            }
        }
    }

    private fun loadLocationAddress(latLng: LatLng) {
        viewModelScope.launch {
            placeRepository.reverseGeocode(latLng)
                .onSuccess { result ->
                    val addr = result.address
                    val displayName = listOfNotNull(
                        addr?.suburb ?: addr?.road,
                        addr?.city
                    ).joinToString(", ").ifBlank { result.displayName?.split(",")?.take(2)?.joinToString(",") ?: "" }
                    _uiState.update { it.copy(locationAddress = displayName) }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchOnMap(query)
    }

    fun searchOnMap(query: String, serviceFilter: String? = null) {
        val latLng = _uiState.value.currentLocation ?: locationRepository.currentLocation.value ?: LatLng(17.3850, 78.4867)
        viewModelScope.launch {
            val q = query.trim()
            if (q.isBlank() && serviceFilter == null) {
                loadNearbyData(latLng)
                return@launch
            }
            // Fast Beast Search for both Vendors & Places on the Map
            geminiRepository.beastSearch(
                query = q,
                category = serviceFilter,
                tab = "ALL",
                latLng = latLng,
                radiusKm = 15.0
            ).onSuccess { resp ->
                if (resp.vendors.isNotEmpty() || resp.places.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            nearbyProviders = resp.vendors,
                            nearbyPlaces = resp.places
                        )
                    }
                    return@launch
                }
            }
            // Fallback to local smart vendor ranker
            providerRepository.searchNearbyProviders(
                userLatLng = latLng,
                serviceId = serviceFilter,
                query = q,
                radiusKm = 15.0,
                limit = 20
            ).onSuccess { providers ->
                _uiState.update { it.copy(nearbyProviders = providers) }
            }
        }
    }

    fun refreshLiveLocation(onLocated: (LatLng) -> Unit = {}) {
        viewModelScope.launch {
            val loc = locationRepository.getLastKnownLocation()
                ?: locationRepository.currentLocation.value
                ?: _uiState.value.currentLocation
                ?: LatLng(17.3850, 78.4867)
            _uiState.update { it.copy(currentLocation = loc) }
            loadNearbyData(loc)
            loadLocationAddress(loc)
            onLocated(loc)
        }
        viewModelScope.launch {
            locationRepository.startLocationUpdates().collect { liveLoc ->
                _uiState.update { it.copy(currentLocation = liveLoc) }
            }
        }
    }

    fun setLocation(latLng: LatLng) {
        _uiState.update { it.copy(currentLocation = latLng) }
        loadNearbyData(latLng)
        loadLocationAddress(latLng)
    }

    fun refresh() {
        val latLng = _uiState.value.currentLocation ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val syncResult = providerRepository.syncProvidersWithSupabase()
            if (syncResult.isSuccess) {
                providerRepository.searchNearbyProviders(latLng, radiusKm = 10.0, limit = 10)
                    .onSuccess { providers ->
                        _uiState.update { it.copy(nearbyProviders = providers) }
                    }
            } else {
                _uiState.update { it.copy(error = "Failed to refresh providers") }
            }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun startVendorChat(providerId: String, onReady: (String) -> Unit) {
        viewModelScope.launch {
            val bookingId = bookingRepository.getOrCreateDirectInquiryBooking(providerId)
            onReady(bookingId)
        }
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }
}

