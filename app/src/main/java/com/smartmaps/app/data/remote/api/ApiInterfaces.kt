package com.smartmaps.app.data.remote.api

import com.smartmaps.app.data.remote.model.*
import retrofit2.Response
import retrofit2.http.*

// ─────────────────────────────────────────────────────────────
// SmartMaps Agentic Backend API (Google ADK + Beast Search)
// Zero client-side API keys — all secrets managed on backend
// ─────────────────────────────────────────────────────────────
interface BackendApi {

    @POST("api/chat")
    suspend fun chat(
        @Body request: BackendChatRequest
    ): BackendChatResponse

    @POST("api/search")
    suspend fun beastSearch(
        @Body request: BackendSearchRequest
    ): BackendSearchResponse

    @GET("api/vendors")
    suspend fun getVendors(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("serviceId") serviceId: String? = null,
        @Query("query") query: String = "",
        @Query("radiusKm") radiusKm: Double = 20.0
    ): BackendVendorsResponse

    @GET("api/vendors/{vendorId}")
    suspend fun getVendorDetail(
        @Path("vendorId") vendorId: String,
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null
    ): ProviderInfo

    @POST("api/vendors")
    suspend fun saveVendor(
        @Body request: BackendVendorCreateRequest
    ): ProviderInfo

    @PATCH("api/vendors/{vendorId}/availability")
    suspend fun updateVendorAvailability(
        @Path("vendorId") vendorId: String,
        @Body request: BackendAvailabilityRequest
    ): ProviderInfo

    @GET("api/bookings")
    suspend fun getBookings(
        @Query("userId") userId: String? = null,
        @Query("providerId") providerId: String? = null
    ): BackendBookingsListResponse

    @POST("api/bookings")
    suspend fun createBooking(
        @Body request: BackendBookingCreateRequest
    ): BackendBookingDto

    @PATCH("api/bookings/{bookingId}/status")
    suspend fun updateBookingStatus(
        @Path("bookingId") bookingId: String,
        @Body request: BackendBookingStatusRequest
    ): BackendBookingDto

    @GET("api/bookings/{bookingId}/messages")
    suspend fun getBookingMessages(
        @Path("bookingId") bookingId: String
    ): BackendMessagesResponse

    @POST("api/bookings/{bookingId}/messages")
    suspend fun sendBookingMessage(
        @Path("bookingId") bookingId: String,
        @Body request: BackendSendMessageRequest
    ): BackendMessagesResponse

    @POST("api/reviews")
    suspend fun submitReview(
        @Body request: BackendReviewCreateRequest
    ): Response<Unit>

    @POST("api/auth/signup")
    suspend fun signup(
        @Body request: BackendAuthSignupRequest
    ): Response<BackendAuthUserResponse>

    @POST("api/auth/login")
    suspend fun login(
        @Body request: BackendAuthLoginRequest
    ): Response<BackendAuthUserResponse>

    @GET("api/reverse-geocode")
    suspend fun reverseGeocode(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): NominatimResult
}

// ─────────────────────────────────────────────────────────────
// Overpass API (OpenStreetMap place search fallback)
// ─────────────────────────────────────────────────────────────
interface OverpassApi {
    @POST("interpreter")
    @FormUrlEncoded
    suspend fun query(
        @Field("data") query: String
    ): OverpassResponse
}

// ─────────────────────────────────────────────────────────────
// Nominatim API (geocoding fallback)
// ─────────────────────────────────────────────────────────────
interface NominatimApi {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 5,
        @Query("addressdetails") addressdetails: Int = 1
    ): List<NominatimResult>

    @GET("reverse")
    suspend fun reverse(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("format") format: String = "json",
        @Query("addressdetails") addressdetails: Int = 1
    ): NominatimResult
}
