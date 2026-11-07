package com.smartmaps.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.smartmaps.app.ui.screens.*
import com.smartmaps.app.ui.viewmodel.AuthViewModel

data class BottomNavItem(
    val label: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

// Consumer: 4 tabs (Vendors & AI Chat centered)
val consumerNavItems = listOf(
    BottomNavItem("Home", Screen.Home.route, Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem("Vendors", Screen.Explore.route, Icons.Filled.Engineering, Icons.Outlined.Engineering),
    BottomNavItem("ADK Chat", Screen.Chat.route, Icons.Filled.SmartToy, Icons.Outlined.SmartToy),
    BottomNavItem("Profile", Screen.Profile.route, Icons.Filled.Person, Icons.Outlined.Person),
)

// Provider: 4 tabs
val providerNavItems = listOf(
    BottomNavItem("Vendor Hub", Screen.ProviderDashboard.route, Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    BottomNavItem("Bookings", Screen.Bookings.route, Icons.Filled.BookOnline, Icons.Outlined.BookOnline),
    BottomNavItem("ADK Chat", Screen.Chat.route, Icons.Filled.SmartToy, Icons.Outlined.SmartToy),
    BottomNavItem("Profile", Screen.Profile.route, Icons.Filled.Person, Icons.Outlined.Person),
)

// Routes that should NOT show bottom nav
val noBottomNavRoutes = setOf(
    Screen.Login.route,
    Screen.Signup.route,
    Screen.ProviderDetail.route,
    Screen.PlaceDetail.route,
    Screen.CreateBooking.route,
    Screen.WriteReview.route,
    Screen.BookingDetail.route,
    Screen.Messages.route,
    Screen.AiSearch.route,
    Screen.ProviderRegistration.route
)

@Composable
fun SmartMapsNavHost(navController: NavHostController) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        // ── Auth Screens ───────────────────────────────────────
        composable(Screen.Login.route) {
            LaunchedEffect(authState.isLoggedIn) {
                if (authState.isLoggedIn) {
                    val dest = if (authState.session?.role == "provider") Screen.ProviderDashboard.route else Screen.Home.route
                    navController.navigate(dest) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            }

            LoginScreen(
                onLogin = { username, password -> authViewModel.login(username, password) },
                onNavigateToSignup = {
                    navController.navigate(Screen.Signup.route)
                },
                isLoading = authState.isLoading,
                error = authState.error,
                onDismissError = { authViewModel.dismissError() }
            )
        }

        composable(Screen.Signup.route) {
            LaunchedEffect(authState.signupSuccess) {
                if (authState.signupSuccess && authState.isLoggedIn) {
                    val dest = if (authState.session?.role == "provider") Screen.ProviderDashboard.route else Screen.Home.route
                    navController.navigate(dest) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            }

            SignupScreen(
                onSignup = { u, p, n, ph, r -> authViewModel.signup(u, p, n, ph, r) },
                onNavigateToLogin = { navController.popBackStack() },
                isLoading = authState.isLoading,
                error = authState.error,
                onDismissError = { authViewModel.dismissError() }
            )
        }

        // ── Consumer: Home ─────────────────────────────────────
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToChat = { navController.navigate(Screen.Chat.route) },
                onNavigateToExplore = { navController.navigate(Screen.Explore.route) },
                onNavigateToProvider = { id -> navController.navigate(Screen.ProviderDetail.createRoute(id)) },
                onNavigateToPlace = { id -> navController.navigate(Screen.PlaceDetail.createRoute(id)) },
                onNavigateToMap = { navController.navigate(Screen.Map.route) },
                onNavigateToAiSearch = { navController.navigate(Screen.AiSearch.createRoute("")) },
                onNavigateToProviderRegistration = { navController.navigate(Screen.ProviderRegistration.route) },
                onNavigateToBooking = { pId, sId -> navController.navigate(Screen.CreateBooking.createRoute(pId, sId)) },
                onNavigateToMessages = { bookingId -> navController.navigate(Screen.Messages.createRoute(bookingId)) }
            )
        }

        // ── Map ────────────────────────────────────────────────
        composable(Screen.Map.route) {
            MapScreen(
                onNavigateToProvider = { id -> navController.navigate(Screen.ProviderDetail.createRoute(id)) },
                onNavigateToPlace = { id -> navController.navigate(Screen.PlaceDetail.createRoute(id)) },
                onNavigateToBooking = { pId, sId -> navController.navigate(Screen.CreateBooking.createRoute(pId, sId)) },
                onNavigateToMessages = { bookingId -> navController.navigate(Screen.Messages.createRoute(bookingId)) }
            )
        }

        // ── Explore (Vendors & Places) ─────────────────────────
        composable(Screen.Explore.route) {
            ExploreScreen(
                onNavigateToProvider = { id -> navController.navigate(Screen.ProviderDetail.createRoute(id)) },
                onNavigateToPlace = { id -> navController.navigate(Screen.PlaceDetail.createRoute(id)) },
                onNavigateToChat = { navController.navigate(Screen.Chat.route) },
                onNavigateToAiSearch = { navController.navigate(Screen.AiSearch.createRoute("")) },
                onNavigateToProviderRegistration = { navController.navigate(Screen.ProviderRegistration.route) },
                onNavigateToMessages = { bookingId -> navController.navigate(Screen.Messages.createRoute(bookingId)) },
                onNavigateToBooking = { pId, sId -> navController.navigate(Screen.CreateBooking.createRoute(pId, sId)) }
            )
        }

        // ── Google ADK AI Chat (Session Context + Maps + Vendors) ─
        composable(Screen.Chat.route) {
            ChatScreen(
                onNavigateToProvider = { id -> navController.navigate(Screen.ProviderDetail.createRoute(id)) },
                onNavigateToPlace = { id -> navController.navigate(Screen.PlaceDetail.createRoute(id)) },
                onNavigateToMap = { navController.navigate(Screen.Map.route) },
                onNavigateToMessages = { bookingId -> navController.navigate(Screen.Messages.createRoute(bookingId)) },
                onNavigateToBooking = { pId, sId -> navController.navigate(Screen.CreateBooking.createRoute(pId, sId)) }
            )
        }

        // ── Beast Hybrid Search ────────────────────────────────
        composable(
            route = Screen.AiSearch.route,
            arguments = listOf(navArgument("query") {
                type = NavType.StringType; nullable = true; defaultValue = null
            })
        ) { backStackEntry ->
            val query = backStackEntry.arguments?.getString("query")
            AiSearchScreen(
                initialQuery = query,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlace = { id -> navController.navigate(Screen.PlaceDetail.createRoute(id)) },
                onNavigateToProvider = { id -> navController.navigate(Screen.ProviderDetail.createRoute(id)) },
                onNavigateToBooking = { pId, sId -> navController.navigate(Screen.CreateBooking.createRoute(pId, sId)) },
                onNavigateToMessages = { bookingId -> navController.navigate(Screen.Messages.createRoute(bookingId)) }
            )
        }

        // ── Bookings ───────────────────────────────────────────
        composable(Screen.Bookings.route) {
            BookingsScreen(
                onNavigateToBookingDetail = { id -> navController.navigate(Screen.BookingDetail.createRoute(id)) },
                onNavigateToMessages = { id -> navController.navigate(Screen.Messages.createRoute(id)) },
                onNavigateToReview = { bId, pId -> navController.navigate(Screen.WriteReview.createRoute(bId, pId)) }
            )
        }

        composable(
            route = Screen.BookingDetail.route,
            arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getString("bookingId") ?: return@composable
            BookingDetailScreen(
                bookingId = bookingId,
                onNavigateToMessages = { navController.navigate(Screen.Messages.createRoute(bookingId)) },
                onNavigateToReview = { bId, pId -> navController.navigate(Screen.WriteReview.createRoute(bId, pId)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Messages.route,
            arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getString("bookingId") ?: return@composable
            MessagesScreen(bookingId = bookingId, onBack = { navController.popBackStack() })
        }

        // ── Profile ────────────────────────────────────────────
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToProviderRegistration = { navController.navigate(Screen.ProviderRegistration.route) },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── Provider Detail ────────────────────────────────────
        composable(
            route = Screen.ProviderDetail.route,
            arguments = listOf(navArgument("providerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val providerId = backStackEntry.arguments?.getString("providerId") ?: return@composable
            ProviderDetailScreen(
                providerId = providerId,
                onNavigateToBooking = { pId, sId -> navController.navigate(Screen.CreateBooking.createRoute(pId, sId)) },
                onNavigateToMessages = { bookingId -> navController.navigate(Screen.Messages.createRoute(bookingId)) },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Place Detail ───────────────────────────────────────
        composable(
            route = Screen.PlaceDetail.route,
            arguments = listOf(navArgument("placeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val placeId = backStackEntry.arguments?.getString("placeId") ?: return@composable
            PlaceDetailScreen(placeId = placeId, onBack = { navController.popBackStack() })
        }

        // ── Provider Registration ──────────────────────────────
        composable(Screen.ProviderRegistration.route) {
            ProviderRegistrationScreen(onBack = { navController.popBackStack() })
        }

        // ── Create Booking ─────────────────────────────────────
        composable(
            route = Screen.CreateBooking.route,
            arguments = listOf(
                navArgument("providerId") { type = NavType.StringType },
                navArgument("serviceId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val providerId = backStackEntry.arguments?.getString("providerId") ?: return@composable
            val serviceId = backStackEntry.arguments?.getString("serviceId") ?: return@composable
            CreateBookingScreen(
                providerId = providerId,
                serviceId = serviceId,
                onSuccess = { bookingId ->
                    navController.navigate(Screen.Messages.createRoute(bookingId)) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Write Review ───────────────────────────────────────
        composable(
            route = Screen.WriteReview.route,
            arguments = listOf(
                navArgument("bookingId") { type = NavType.StringType },
                navArgument("providerId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getString("bookingId") ?: return@composable
            val providerId = backStackEntry.arguments?.getString("providerId") ?: return@composable
            WriteReviewScreen(bookingId = bookingId, providerId = providerId, onBack = { navController.popBackStack() })
        }

        // ── Provider Dashboard ─────────────────────────────────
        composable(Screen.ProviderDashboard.route) {
            ProviderDashboardScreen(
                onNavigateToRegistration = { navController.navigate(Screen.ProviderRegistration.route) },
                onNavigateToBookings = { navController.navigate(Screen.Bookings.route) },
                onNavigateToMessages = { navController.navigate(Screen.Bookings.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToBookingChat = { bookingId -> navController.navigate(Screen.Messages.createRoute(bookingId)) }
            )
        }
    }
}
