package com.smartmaps.app.data.remote.model

import com.google.gson.annotations.SerializedName

// ─────────────────────────────────────────────────────────────
// Backend Agentic Platform (Google ADK + Beast Search) Models
// ─────────────────────────────────────────────────────────────

data class BackendChatRequest(
    val sessionId: String? = null,
    val userId: String = "local_user",
    val message: String,
    val latitude: Double = 17.3850,
    val longitude: Double = 78.4867
)

data class BackendChatResponse(
    val sessionId: String,
    val reply: String,
    val intentResult: IntentResult? = null,
    val suggestedProviders: List<ProviderInfo> = emptyList(),
    val suggestedPlaces: List<Place> = emptyList(),
    val mapContext: MapContextInfo? = null,
    val quickReplies: List<String> = emptyList(),
    val agentEngine: String = "Google ADK"
)

data class MapMarkerInfo(
    val id: String,
    val type: String, // "vendor" | "place"
    val title: String,
    val subtitle: String = "",
    val lat: Double,
    val lng: Double,
    val rating: Float = 4.8f
)

data class MapContextInfo(
    val centerLat: Double = 17.3850,
    val centerLng: Double = 78.4867,
    val zoom: Double = 14.0,
    val markers: List<MapMarkerInfo> = emptyList(),
    val hasVendorPins: Boolean = false,
    val hasPlacePins: Boolean = false
)

data class BackendSearchRequest(
    val query: String = "",
    val category: String? = null,
    val tab: String = "ALL", // "ALL" | "SERVICES" | "PLACES"
    val latitude: Double = 17.3850,
    val longitude: Double = 78.4867,
    val radiusKm: Double = 10.0,
    val sortByDistance: Boolean = FalseDefault.VALUE,
    val urgency: String = "normal",
    val limit: Int = 30
) {
    private object FalseDefault { const val VALUE = false }
}

data class BackendSearchResponse(
    val query: String = "",
    val latencyMs: Double = 0.0,
    val analysisText: String = "",
    val intent: IntentResult? = null,
    val vendors: List<ProviderInfo> = emptyList(),
    val places: List<Place> = emptyList()
)

data class BackendVendorsResponse(
    val vendors: List<ProviderInfo> = emptyList()
)

data class BackendVendorCreateRequest(
    val id: String? = null,
    val name: String,
    val businessName: String,
    val phone: String,
    val email: String = "",
    val description: String = "",
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val serviceRadius: Double = 15.0,
    val experienceYears: Int = 3,
    val priceRangeMin: Int = 250,
    val priceRangeMax: Int = 1000,
    val isAvailable: Boolean = true,
    val services: List<String> = emptyList()
)

data class BackendAvailabilityRequest(
    val isAvailable: Boolean
)

data class BackendBookingCreateRequest(
    val id: String? = null,
    val userId: String = "local_user",
    val userName: String = "Customer",
    val userPhone: String = "",
    val providerId: String,
    val serviceId: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val scheduledAt: Long,
    val notes: String = "",
    val urgency: String = "normal",
    val estimatedCost: Int = 0
)

data class BackendBookingDto(
    val id: String,
    val userId: String = "local_user",
    val userName: String = "Customer",
    val userPhone: String = "",
    val providerId: String,
    val providerName: String = "",
    val serviceId: String,
    val serviceName: String = "",
    val status: String = "PENDING",
    val scheduledAt: Long = System.currentTimeMillis(),
    val address: String = "",
    val latitude: Double = 17.3850,
    val longitude: Double = 78.4867,
    val notes: String = "",
    val urgency: String = "normal",
    val estimatedCost: Int = 0,
    val quotedCost: Int = 0,
    val actualCost: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class BackendBookingsListResponse(
    val bookings: List<BackendBookingDto> = emptyList()
)

data class BackendBookingStatusRequest(
    val status: String,
    val quotedCost: Int? = null
)

data class BackendMessageDto(
    val id: String,
    val bookingId: String,
    val senderId: String,
    val senderType: String, // "user" | "provider"
    val senderName: String = "",
    val content: String,
    val messageType: String = "text",
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class BackendMessagesResponse(
    val bookingId: String,
    val messages: List<BackendMessageDto> = emptyList()
)

data class BackendSendMessageRequest(
    val senderId: String = "local_user",
    val senderType: String = "user",
    val senderName: String = "Customer",
    val content: String,
    val messageType: String = "text"
)

data class BackendAuthSignupRequest(
    val username: String,
    val password: String,
    val name: String,
    val phone: String,
    val role: String
)

data class BackendAuthLoginRequest(
    val username: String,
    val password: String
)

data class BackendAuthUserResponse(
    val userId: String,
    val username: String,
    val name: String,
    val phone: String,
    val role: String
)

data class BackendReviewCreateRequest(
    val providerId: String,
    val userId: String = "local_user",
    val bookingId: String,
    val rating: Float,
    val comment: String
)

// ─────────────────────────────────────────────────────────────
// Overpass & Nominatim API Models
// ─────────────────────────────────────────────────────────────
data class OverpassResponse(
    val elements: List<OverpassElement>
)

data class OverpassElement(
    val type: String,          // "node", "way", "relation"
    val id: Long,
    val lat: Double?,
    val lon: Double?,
    val center: OverpassCenter?,
    val tags: Map<String, String>?
)

data class OverpassCenter(
    val lat: Double,
    val lon: Double
)

data class NominatimResult(
    @SerializedName("place_id") val placeId: Long?,
    @SerializedName("display_name") val displayName: String?,
    val lat: String?,
    val lon: String?,
    val address: NominatimAddress?,
    val importance: Double?
)

data class NominatimAddress(
    val road: String?,
    val suburb: String?,
    val city: String?,
    val state: String?,
    val country: String?,
    @SerializedName("postcode") val postcode: String?
)

// ─────────────────────────────────────────────────────────────
// Domain Models
// ─────────────────────────────────────────────────────────────
data class LatLng(val lat: Double, val lng: Double)

data class Place(
    val id: String,
    val name: String,
    val categoryKey: String,
    val categoryLabel: String,
    val latLng: LatLng,
    val address: String = "",
    val phone: String = "",
    val website: String = "",
    val openingHours: String = "",
    val rating: Float = 0f,
    val reviewCount: Int = 0,
    val priceLevel: Int = 0,
    val isOpen: Boolean? = null,
    val distanceMeters: Double = 0.0,
    val source: String = "osm"
)

data class ServiceInfo(
    val id: String,
    val name: String,
    val categoryId: String,
    val categoryName: String,
    val description: String,
    val icon: String,
    val estimatedLabourMin: Int,
    val estimatedLabourMax: Int,
    val estimatedDurationMin: Int,
    val estimatedDurationMax: Int,
    val possibleWorkers: Int
)

data class ProviderInfo(
    val id: String,
    val name: String,
    val businessName: String,
    val phone: String,
    val description: String,
    val profileImageUrl: String = "",
    val latLng: LatLng,
    val address: String,
    val rating: Float,
    val reviewCount: Int,
    val completedJobs: Int,
    val isAvailable: Boolean,
    val isVerified: Boolean,
    val distanceMeters: Double = 0.0,
    val estimatedArrivalMinutes: Int = 10,
    val services: List<String> = emptyList(),
    val matchScore: Int = 95,
    val matchReason: String = "",
    val priceRangeMin: Int = 250,
    val priceRangeMax: Int = 1000,
    val experienceYears: Int = 5
)

data class BookingInfo(
    val id: String,
    val providerId: String,
    val serviceId: String,
    val serviceName: String,
    val providerName: String,
    val status: BookingStatus,
    val scheduledAt: Long,
    val address: String,
    val notes: String,
    val estimatedCost: Int,
    val actualCost: Int,
    val createdAt: Long
)

enum class BookingStatus {
    PENDING, ACCEPTED, REJECTED, CANCELLED,
    ON_THE_WAY, ARRIVED, IN_PROGRESS, COMPLETED, DISPUTED
}

// AI intent classification result
data class IntentResult(
    val intent: String = "service_request", // "service_request" | "place_search" | "navigation" | "general"
    val category: String? = null,
    val categoryName: String? = null,
    val placeCategories: List<String>? = null,
    val service: String? = null,
    val serviceName: String? = null,
    val query: String? = null,
    val problem: String? = null,
    val urgency: String? = "normal",
    val workersRequired: Int? = 1,
    val estimatedDurationMin: Int? = 30,
    val estimatedDurationMax: Int? = 90,
    val estimatedLabourMin: Int? = 250,
    val estimatedLabourMax: Int? = 900,
    val requiredSkills: List<String> = emptyList(),
    val confidence: Float = 0.9f,
    val clarificationNeeded: Boolean = false,
    val clarificationQuestion: String? = null,
    val radiusKm: Double? = 10.0,
    val timeContext: String? = null,
    val pricePreference: String? = null
)

data class ChatMessage(
    val id: String,
    val role: String,   // "user" | "model"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val intentResult: IntentResult? = null
)
