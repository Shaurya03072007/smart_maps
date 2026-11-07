package com.smartmaps.app.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Signup : Screen("signup")
    object Home : Screen("home")
    object Map : Screen("map")
    object Explore : Screen("explore")
    object Chat : Screen("chat")
    object AiSearch : Screen("ai_search?query={query}") {
        fun createRoute(query: String = "") = if (query.isNotBlank()) "ai_search?query=$query" else "ai_search"
    }
    object Bookings : Screen("bookings")
    object BookingDetail : Screen("booking/{bookingId}") {
        fun createRoute(bookingId: String) = "booking/$bookingId"
    }
    object Messages : Screen("messages/{bookingId}") {
        fun createRoute(bookingId: String) = "messages/$bookingId"
    }
    object Profile : Screen("profile")
    object ProviderDetail : Screen("provider/{providerId}") {
        fun createRoute(providerId: String) = "provider/$providerId"
    }
    object PlaceDetail : Screen("place/{placeId}") {
        fun createRoute(placeId: String) = "place/$placeId"
    }
    object ProviderRegistration : Screen("provider_registration")
    object ProviderDashboard : Screen("provider_dashboard")
    object CreateBooking : Screen("create_booking/{providerId}/{serviceId}") {
        fun createRoute(providerId: String, serviceId: String) = "create_booking/$providerId/$serviceId"
    }
    object WriteReview : Screen("review/{bookingId}/{providerId}") {
        fun createRoute(bookingId: String, providerId: String) = "review/$bookingId/$providerId"
    }
}
