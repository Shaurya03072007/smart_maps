package com.smartmaps.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartmaps.app.data.local.entity.BookingEntity
import com.smartmaps.app.data.local.entity.MessageEntity
import com.smartmaps.app.data.remote.model.BookingStatus
import com.smartmaps.app.data.repository.BookingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookingUiState(
    val bookings: List<BookingEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            launch { bookingRepository.syncBookingsFromBackend() }
            bookingRepository.getAllBookings().collect { bookings ->
                _uiState.update { it.copy(bookings = bookings) }
            }
        }
    }

    fun getMessagesForBooking(bookingId: String): Flow<List<MessageEntity>> {
        viewModelScope.launch {
            bookingRepository.syncMessagesForBooking(bookingId)
        }
        return bookingRepository.getMessages(bookingId)
    }

    fun sendMessage(bookingId: String, text: String, senderType: String = "user") {
        if (text.isBlank()) return
        viewModelScope.launch {
            bookingRepository.sendMessage(bookingId = bookingId, text = text.trim(), senderType = senderType)
        }
    }

    fun updateStatus(bookingId: String, status: BookingStatus, quotedCost: Int? = null) {
        viewModelScope.launch {
            bookingRepository.updateBookingStatus(bookingId, status, quotedCost)
        }
    }

    fun createBooking(
        providerId: String,
        serviceId: String,
        address: String,
        latitude: Double,
        longitude: Double,
        scheduledAt: Long,
        notes: String,
        estimatedCost: Int,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            bookingRepository.createBooking(
                providerId, serviceId, address, latitude, longitude,
                scheduledAt, notes, estimatedCost
            ).onSuccess { id ->
                onSuccess(id)
            }.onFailure { error ->
                _uiState.update { it.copy(error = error.message) }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }
}
