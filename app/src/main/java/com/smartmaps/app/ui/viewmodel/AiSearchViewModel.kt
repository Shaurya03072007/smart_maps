package com.smartmaps.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartmaps.app.data.location.LocationRepository
import com.smartmaps.app.data.remote.model.IntentResult
import com.smartmaps.app.data.remote.model.LatLng
import com.smartmaps.app.data.remote.model.Place
import com.smartmaps.app.data.remote.model.ProviderInfo
import com.smartmaps.app.data.repository.BookingRepository
import com.smartmaps.app.data.repository.GeminiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiSearchUiState(
    val query: String = "",
    val isAnalyzing: Boolean = false,
    val isSearching: Boolean = false,
    val latencyMs: Double = 0.0,
    val analysisText: String = "",
    val intent: IntentResult? = null,
    val vendors: List<ProviderInfo> = emptyList(),
    val places: List<Place> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class AiSearchViewModel @Inject constructor(
    private val geminiRepository: GeminiRepository,
    private val locationRepository: LocationRepository,
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiSearchUiState())
    val uiState: StateFlow<AiSearchUiState> = _uiState.asStateFlow()
    private var debounceJob: Job? = null

    init {
        // Pre-populate top nearby vendors immediately so search screen is rich on open
        executeSearch("")
    }

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(140) // Ultra-responsive 140ms debounce
            executeSearch(newQuery)
        }
    }

    fun search() {
        debounceJob?.cancel()
        executeSearch(_uiState.value.query)
    }

    private fun executeSearch(queryText: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSearching = true,
                    error = null
                )
            }

            val currentLocation = locationRepository.currentLocation.value ?: LatLng(17.3850, 78.4867)

            val result = geminiRepository.beastSearch(
                query = queryText,
                tab = "ALL",
                latLng = currentLocation,
                radiusKm = 15.0
            )

            result.onSuccess { resp ->
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        isSearching = false,
                        latencyMs = resp.latencyMs,
                        analysisText = resp.analysisText,
                        intent = resp.intent,
                        vendors = resp.vendors,
                        places = resp.places
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        isSearching = false,
                        error = "Search error: ${err.message}"
                    )
                }
            }
        }
    }

    fun openVendorDirectChat(providerId: String, onReady: (String) -> Unit) {
        viewModelScope.launch {
            val bookingId = bookingRepository.getOrCreateDirectInquiryBooking(providerId)
            onReady(bookingId)
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}
