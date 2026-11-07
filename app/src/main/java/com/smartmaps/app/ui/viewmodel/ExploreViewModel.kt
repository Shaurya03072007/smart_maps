package com.smartmaps.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartmaps.app.data.local.dao.ServiceCategoryDao
import com.smartmaps.app.data.local.dao.ServiceDao
import com.smartmaps.app.data.local.entity.ServiceCategoryEntity
import com.smartmaps.app.data.local.entity.ServiceEntity
import com.smartmaps.app.data.location.LocationRepository
import com.smartmaps.app.data.remote.model.LatLng
import com.smartmaps.app.data.remote.model.Place
import com.smartmaps.app.data.remote.model.ProviderInfo
import com.smartmaps.app.data.repository.BookingRepository
import com.smartmaps.app.data.repository.PlaceRepository
import com.smartmaps.app.data.repository.ProviderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ExploreTab { SERVICES, PLACES }
enum class ResultViewMode { LIST, MAP }

data class ExploreUiState(
    val tab: ExploreTab = ExploreTab.SERVICES, // Default to Vendors & Services!
    val viewMode: ResultViewMode = ResultViewMode.LIST,
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val radiusKm: Double = 10.0,
    val sortByDistance: Boolean = false,
    val onlyOpen: Boolean = false,
    val places: List<Place> = emptyList(),
    val providers: List<ProviderInfo> = emptyList(),
    val services: List<ServiceEntity> = emptyList(),
    val serviceCategories: List<ServiceCategoryEntity> = emptyList(),
    val placeCategories: List<String> = PLACE_CATEGORIES,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentLocation: LatLng? = null
) {
    companion object {
        val PLACE_CATEGORIES = listOf(
            "food", "restaurant", "cafe", "hospital", "clinic", "pharmacy",
            "bank", "atm", "supermarket", "fuel", "hotel", "gym",
            "park", "school", "cinema", "salon", "spa", "police", "bus_stop"
        )
    }
}

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val providerRepository: ProviderRepository,
    private val bookingRepository: BookingRepository,
    private val serviceCategoryDao: ServiceCategoryDao,
    private val serviceDao: ServiceDao,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            launch {
                serviceCategoryDao.getAllCategories().collect { cats ->
                    _uiState.update { it.copy(serviceCategories = cats) }
                }
            }
            launch {
                locationRepository.currentLocation.collect { latLng ->
                    val effectiveLoc = latLng ?: LatLng(17.3850, 78.4867)
                    _uiState.update { it.copy(currentLocation = effectiveLoc) }
                    search()
                }
            }
        }
    }

    fun onTabChange(tab: ExploreTab) {
        _uiState.update { it.copy(tab = tab, selectedCategory = null) }
        search()
    }

    fun onViewModeChange(mode: ResultViewMode) = _uiState.update { it.copy(viewMode = mode) }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(140)
            search()
        }
    }

    fun onCategorySelect(category: String?) = _uiState.update { it.copy(selectedCategory = category) }
    fun onRadiusChange(km: Double) = _uiState.update { it.copy(radiusKm = km) }
    fun toggleOpenOnly() = _uiState.update { it.copy(onlyOpen = !it.onlyOpen) }
    fun toggleSortByDistance() = _uiState.update { it.copy(sortByDistance = !it.sortByDistance) }
    fun dismissError() = _uiState.update { it.copy(error = null) }

    fun openVendorDirectChat(providerId: String, onReady: (String) -> Unit) {
        viewModelScope.launch {
            val bookingId = bookingRepository.getOrCreateDirectInquiryBooking(providerId)
            onReady(bookingId)
        }
    }

    fun search() {
        val state = _uiState.value
        val latLng = state.currentLocation ?: LatLng(17.3850, 78.4867)

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (state.tab) {
                ExploreTab.PLACES -> searchPlaces(latLng, state)
                ExploreTab.SERVICES -> searchProviders(latLng, state)
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun searchServices(query: String) {
        viewModelScope.launch {
            val results = serviceDao.searchServices(query)
            _uiState.update { it.copy(services = results, searchQuery = query) }
            search()
        }
    }

    private suspend fun searchPlaces(latLng: LatLng, state: ExploreUiState) {
        val category = state.selectedCategory ?: state.searchQuery.ifBlank { "restaurant" }
        val radiusMeters = (state.radiusKm * 1000).toInt()

        placeRepository.searchNearbyPlaces(latLng, listOf(category), radiusMeters, limit = 50)
            .onSuccess { places ->
                val result = if (state.sortByDistance) {
                    places.sortedBy { it.distanceMeters }
                } else {
                    places.sortedByDescending { it.rating }
                }
                _uiState.update { it.copy(places = result) }
            }
            .onFailure { error ->
                _uiState.update { it.copy(error = error.message ?: "Search failed") }
            }
    }

    private suspend fun searchProviders(latLng: LatLng, state: ExploreUiState) {
        providerRepository.searchNearbyProviders(
            userLatLng = latLng,
            query = state.searchQuery,
            radiusKm = state.radiusKm,
            sortByDistance = state.sortByDistance,
            limit = 40
        ).onSuccess { providers ->
            _uiState.update { it.copy(providers = providers) }
        }.onFailure { error ->
            _uiState.update { it.copy(error = error.message ?: "Search failed") }
        }

        if (state.searchQuery.isNotBlank()) {
            val matchingServices = serviceDao.searchServices(state.searchQuery)
            _uiState.update { it.copy(services = matchingServices) }
        }
    }
}
